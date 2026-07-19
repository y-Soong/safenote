package com.prafta.web.subcon.subcon02.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.application.command.DefectActionWriteCommand;
import com.prafta.web.subcon.subcon02.application.command.InspectAnswerWriteCommand;
import com.prafta.web.subcon.subcon02.application.model.ChkptAnswerChain;
import com.prafta.web.subcon.subcon02.application.model.ChkptChainTier;
import com.prafta.web.subcon.subcon02.application.param.DefectActionPropagateParam;
import com.prafta.web.subcon.subcon02.application.param.InspectAnswerPropagateParam;
import com.prafta.web.subcon.subcon02.mapper.ChkptLinkMapper;
import com.prafta.web.subcon.subcon02.result.ChkptTierRaw;
import com.prafta.web.subcon.subcon02.result.ItemLinkPairRaw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 점검 결과 통합(write-through) 전파 서비스(PRAFTA-SUBCON-T6-05·T6-06, plan §4-2).
 *
 * <p>연동 점검대상은 물리적으로 하나이므로 어느 티어가 점검하든 체인 전 티어(상·하 양방향)에 반영한다
 * (마스터 §1-11 — 스냅샷 프레임의 예외). 대상 좌표는 <b>서버 데이터(LINK_SRC 매핑)로만</b> 산출한다
 * (클라 입력 회사코드 불신).
 *
 * <p>수행 회사 표시는 <b>인접 1차 relabel</b>이다(plan D4): 각 티어 복제행의 PERFORM_CMPNY_CD 는
 * 원 수행 회사가 아니라 BFS 진입 방향의 직전 노드 회사다 → 2차 이하 회사코드가 조상 테넌트에 남지 않는다.
 *
 * <p>[qa M-3] 체인 해석은 <b>점검대상(chkpt) 단위로 1회</b>만 수행한다({@link #openChain}).
 * 점검대상 매핑은 저장 1회 동안 전 문항에 대해 불변이므로, 문항마다 (부모 1 + 자식 N) 링크를 재조회하지 않는다.
 * 문항 좌표는 체인 간선당 1회 조회한 매핑표로 치환한다.
 *
 * <p>전파는 동기 + 호출자 트랜잭션 참여다(실패 = 원본 저장 실패로 표면화).
 * 단 물리 파일 I/O 는 트랜잭션 밖이므로 롤백 시 고아 파일이 남을 수 있다(현행 fileSave 와 동일 특성).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectAnswerPropagationService {

    private final ChkptLinkMapper chkptLinkMapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    /** PRAFTA-SUBCON-T6-AUDIT-02: 전파 각 티어의 덮어쓰기 감사 이력 캡처(W2 응답 / W4 불량조치). */
    private final ChkptResultHistRecorder chkptResultHistRecorder;

    /** 체인 깊이 안전핀 — 초과 시 데이터 오염(루프) 감지로 전체 롤백. */
    private static final int MAX_CHAIN_DEPTH = 20;

    /** 점검 사진 파일 유형(001: 일일점검 — 앱 chkLst01 과 동일). */
    private static final String FILE_TYPE_DAILY_INSPECT = "001";

    /** 불량조치 사진 파일 유형(006: 점검조치사진 — SYS010, 점검 사진 001 과 구분). */
    private static final String FILE_TYPE_DEFECT_ACTION = "006";

    /** media_type → 확장자(FileService 이미지 화이트리스트와 동일 집합). */
    private static final Map<String, String> EXT_BY_MEDIA_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    /**
     * [qa M-3] 기점 점검대상 기준 체인 스냅샷 해석(저장 1회당 1번만 호출한다).
     *
     * <p>점검대상 BFS(부모 1홉 + 자식 N홉)로 티어를 수집하고, 각 간선마다 사이트 쌍 문항 매핑표를 1회 조회해
     * "기점 문항코드 → 그 티어 문항코드" 사전을 만든다. 매핑이 없으면 빈 체인(= 연동되지 않은 자체 점검대상).
     */
    public ChkptAnswerChain openChain(String cmpnyCd, String siteCd, String chkptCd) {

        String chkLstType = chkptLinkMapper.selectChkptChkLstType(cmpnyCd, siteCd, chkptCd);
        if (chkLstType == null || chkLstType.isBlank()) {
            return new ChkptAnswerChain(List.of());
        }

        List<ChkptChainTier> tiers = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(chkptKey(cmpnyCd, siteCd, chkptCd));

        Deque<BfsNode> queue = new ArrayDeque<>();
        // 기점 노드의 문항 매핑은 항등(identity) — null 로 표현한다(기점 문항 목록을 조회할 필요가 없다).
        queue.add(new BfsNode(cmpnyCd, siteCd, chkLstType, chkptCd, cmpnyCd, 0, null));

        while (!queue.isEmpty()) {
            BfsNode current = queue.poll();

            if (current.depth() > MAX_CHAIN_DEPTH) {
                log.error("점검 결과 전파 깊이 초과(데이터 오염 의심) - 기점={}:{}:{}", cmpnyCd, siteCd, chkptCd);
                throw new ApiException(SubconErrorCode.SUBCON_500_001);
            }

            // 부모(1홉 위) — 현재 티어 문항의 LINK_SRC_ITEM_CD 가 부모 문항이다(현재 → 부모 방향 그대로).
            ChkptTierRaw parent = chkptLinkMapper.selectChkptParentTier(
                    current.cmpnyCd(), current.siteCd(), current.chkptCd());
            if (parent != null) {
                Map<String, String> edge = toEdgeMap(chkptLinkMapper.selectItemLinkPairs(
                        current.cmpnyCd(), current.siteCd(), current.chkLstType(),
                        parent.cmpnyCd(), parent.siteCd()), false);
                enqueueIfNew(queue, tiers, visited, parent, current, edge);
            }

            // 자식(1홉 아래) — 자식 문항의 LINK_SRC_ITEM_CD 가 현재 문항이므로 역방향(현재 → 자식)으로 뒤집는다.
            List<ChkptTierRaw> children = chkptLinkMapper.selectChkptChildTiers(
                    current.cmpnyCd(), current.siteCd(), current.chkptCd());
            for (ChkptTierRaw child : children) {
                Map<String, String> edge = toEdgeMap(chkptLinkMapper.selectItemLinkPairs(
                        child.cmpnyCd(), child.siteCd(), child.chkLstType(),
                        current.cmpnyCd(), current.siteCd()), true);
                enqueueIfNew(queue, tiers, visited, child, current, edge);
            }
        }

        return new ChkptAnswerChain(tiers);
    }

    /**
     * 점검 응답 write-through(단건 호출 진입점) — 체인을 즉석에서 1회 해석한다.
     * 여러 문항을 저장하는 루프에서는 {@link #openChain} 으로 체인을 먼저 열고
     * {@link #propagateAnswer(ChkptAnswerChain, InspectAnswerPropagateParam)} 를 사용한다(N+1 방지).
     */
    @Transactional
    public void propagateAnswer(InspectAnswerPropagateParam param) {
        ChkptAnswerChain chain = openChain(param.cmpnyCd(), param.siteCd(), param.chkptCd());
        propagateAnswer(chain, param);
    }

    /**
     * 점검 응답 write-through — 기점(수행 티어)을 제외한 체인 전 티어의 대응 좌표에 응답 행을 복제한다.
     *
     * <p>[정책 변경 — 후행 덮어쓰기(last-writer-wins)] 선수행 우선/소유 판정(PERFORM_KEY)은 폐기됐다.
     * 각 티어는 대응 좌표에 <b>무조건 UPSERT</b> 한다(선행 데이터가 상위/타사 것이라도 덮어쓴다 — 사용자가 앱
     * 확인 팝업으로 이미 동의한 write-through 동작). 표시용 수행자 스냅샷은 덮어쓸 때마다 최신값으로 갱신된다.
     *
     * <p>사진 복제는 <b>쓰기가 실제로 일어나는 티어에서만</b> 수행한다(M-2 유지). 이제 대응 문항이 있는 티어는
     * 항상 쓰기가 일어나므로(no-op 티어 없음) 대응 티어마다 사진을 복제한다. 대응 문항이 없는 티어는
     * continue 로 건너뛰어 사진 채번/저장 자체를 하지 않는다(고아 파일 방지).
     */
    @Transactional
    public void propagateAnswer(ChkptAnswerChain chain, InspectAnswerPropagateParam param) {

        if (chain == null || chain.isEmpty()) {
            return; // 매핑 없음 = 연동되지 않은(자체) 점검대상 — 자기 테넌트에만 기록.
        }

        for (ChkptChainTier tier : chain.tiers()) {

            String tierItemCd = tier.resolveItemCd(param.inspectItemCd());
            if (tierItemCd == null) {
                // 그 티어에 대응 문항이 없다(수신사 자체 문항 등) — 전파 대상 아님(사진도 건드리지 않는다).
                continue;
            }

            // 사진은 티어별 소유 파일로 복제(원본 회사 파일 삭제와 무관하게 열람 가능 — 엣지 8).
            String tierFileMgmtCd = copyPhotoForTier(
                    param.cmpnyCd(), param.srcFileMgmtCd(), tier.cmpnyCd(), tier.siteCd(), FILE_TYPE_DAILY_INSPECT);

            InspectAnswerWriteCommand command = new InspectAnswerWriteCommand(
                    tier.cmpnyCd()
                    , tier.siteCd()
                    , tier.chkptCd()
                    , tierItemCd
                    , param.workDate()
                    , param.inspectAnswerType()
                    , param.answerDesc()
                    , tierFileMgmtCd
                    , tier.viaCmpnyCd()          // 인접 1차 relabel(plan D4) — 표시 전용
                    , param.performUserCd()
                    , param.performUserNm());

            // PRAFTA-SUBCON-T6-AUDIT-02(W2): 이 티어 좌표의 write 직전 존재여부로 CHG_TYPE 판정.
            //   continue 티어(대응 문항 없음)는 위에서 이미 건너뛰었으므로 여기 도달분만 캡처된다(유령 행 없음).
            boolean tierAnswerExisted = chkptResultHistRecorder.existsAnswer(
                    tier.cmpnyCd(), tier.siteCd(), tier.chkptCd(), tierItemCd, param.workDate());

            int affected = chkptLinkMapper.upsertAnswer(command);

            // W2: write 직후 그 티어의 방금 쓴 행을 HIST 로 append(트리거 주체=SYSTEM — 전파).
            chkptResultHistRecorder.captureAnswer(
                    tier.cmpnyCd(), tier.siteCd(), tier.chkptCd(), tierItemCd, param.workDate(),
                    ChkptResultHistRecorder.chgType(tierAnswerExisted), ChkptResultHistRecorder.INSERT_NO_SYSTEM);

            log.info("점검 응답 전파(덮어쓰기) - {}:{}:{}:{} -> {}:{}:{}:{}, 영향행={}",
                    param.cmpnyCd(), param.siteCd(), param.chkptCd(), param.inspectItemCd(),
                    tier.cmpnyCd(), tier.siteCd(), tier.chkptCd(), tierItemCd, affected);
        }
    }

    /**
     * 불량조치 write-through — 응답과 동일한 체인 좌표 집합에 조치 내역 + 조치 사진을 복제한다.
     *
     * <p>[정책 변경 — 후행 덮어쓰기] 선처리 우선/소유 판정(ACTION_KEY)은 폐기됐다. 각 티어는 대응 좌표에
     * 무조건 UPSERT 한다. 조치 사진(FILE_TYPE 006)도 응답 사진과 동일하게 티어별 소유 파일로 복제한다
     * (쓰기가 일어나는 티어에서만 — 대응 문항 없는 티어는 continue).
     */
    @Transactional
    public void propagateDefectAction(DefectActionPropagateParam param) {

        ChkptAnswerChain chain = openChain(param.cmpnyCd(), param.siteCd(), param.chkptCd());
        if (chain.isEmpty()) {
            return;
        }

        for (ChkptChainTier tier : chain.tiers()) {

            String tierItemCd = tier.resolveItemCd(param.inspectItemCd());
            if (tierItemCd == null) {
                continue;
            }

            String tierFileMgmtCd = copyPhotoForTier(
                    param.cmpnyCd(), param.srcFileMgmtCd(), tier.cmpnyCd(), tier.siteCd(), FILE_TYPE_DEFECT_ACTION);

            DefectActionWriteCommand command = new DefectActionWriteCommand(
                    tier.cmpnyCd()
                    , tier.siteCd()
                    , tier.chkptCd()
                    , tierItemCd
                    , param.workDate()
                    , param.actionDesc()
                    , tierFileMgmtCd
                    , tier.viaCmpnyCd()          // 인접 1차 relabel — 표시 전용
                    , param.actionUserCd()
                    , param.actionUserNm());

            // PRAFTA-SUBCON-T6-AUDIT-02(W4): 이 티어 좌표의 write 직전 존재여부로 CHG_TYPE 판정.
            boolean tierDefectExisted = chkptResultHistRecorder.existsDefectAction(
                    tier.cmpnyCd(), tier.siteCd(), tier.chkptCd(), tierItemCd, param.workDate());

            int affected = chkptLinkMapper.upsertDefectAction(command);

            // W4: write 직후 그 티어의 방금 쓴 조치행을 HIST 로 append(트리거 주체=SYSTEM — 전파).
            chkptResultHistRecorder.captureDefect(
                    tier.cmpnyCd(), tier.siteCd(), tier.chkptCd(), tierItemCd, param.workDate(),
                    ChkptResultHistRecorder.chgType(tierDefectExisted), ChkptResultHistRecorder.INSERT_NO_SYSTEM);

            log.info("불량조치 전파(덮어쓰기) - {}:{}:{}:{} -> {}:{}:{}:{}, 영향행={}",
                    param.cmpnyCd(), param.siteCd(), param.chkptCd(), param.inspectItemCd(),
                    tier.cmpnyCd(), tier.siteCd(), tier.chkptCd(), tierItemCd, affected);
        }
    }

    // =========================== private ===========================

    /**
     * 간선 문항 매핑표 구성.
     *
     * @param pairs   (수신 문항코드, 원본 문항코드) 쌍
     * @param reverse false = 수신→원본(부모 방향), true = 원본→수신(자식 방향)
     */
    private Map<String, String> toEdgeMap(List<ItemLinkPairRaw> pairs, boolean reverse) {
        Map<String, String> edge = new HashMap<>();
        if (pairs == null) {
            return edge;
        }
        for (ItemLinkPairRaw pair : pairs) {
            if (pair.itemCd() == null || pair.srcItemCd() == null) {
                continue;
            }
            if (reverse) {
                edge.put(pair.srcItemCd(), pair.itemCd());
            } else {
                edge.put(pair.itemCd(), pair.srcItemCd());
            }
        }
        return edge;
    }

    /**
     * 인접 티어를 체인에 편입한다(미방문 점검대상만).
     *
     * @param edge 현재 티어 문항코드 → 후보 티어 문항코드 매핑(간선 1회 조회 결과)
     */
    private void enqueueIfNew(Deque<BfsNode> queue, List<ChkptChainTier> tiers, Set<String> visited,
            ChkptTierRaw candidate, BfsNode from, Map<String, String> edge) {

        if (candidate == null || candidate.cmpnyCd() == null || candidate.chkptCd() == null) {
            return;
        }
        String key = chkptKey(candidate.cmpnyCd(), candidate.siteCd(), candidate.chkptCd());
        if (!visited.add(key)) {
            return;
        }

        // 기점 문항 → 후보 티어 문항 매핑 = (기점 → 현재) 합성 (현재 → 후보).
        Map<String, String> itemCdByOrigin = compose(from.itemCdByOriginItemCd(), edge);
        if (itemCdByOrigin.isEmpty()) {
            // 문항 매핑이 하나도 없으면 그 아래 체인도 문항 좌표를 만들 수 없다 — 편입하지 않는다.
            log.info("점검 결과 전파 티어 제외(문항 매핑 없음) - {}:{}:{}",
                    candidate.cmpnyCd(), candidate.siteCd(), candidate.chkptCd());
            return;
        }

        ChkptChainTier tier = new ChkptChainTier(
                candidate.cmpnyCd()
                , candidate.siteCd()
                , candidate.chkptCd()
                , from.cmpnyCd()        // 인접 1차 relabel — 진입 방향의 직전 노드 회사.
                , itemCdByOrigin);

        tiers.add(tier);
        queue.add(new BfsNode(
                candidate.cmpnyCd()
                , candidate.siteCd()
                , candidate.chkLstType()
                , candidate.chkptCd()
                , from.cmpnyCd()
                , from.depth() + 1
                , itemCdByOrigin));
    }

    /**
     * 매핑 합성 — (기점 → 현재) 와 (현재 → 다음) 을 이어 (기점 → 다음) 을 만든다.
     * 기점 노드의 매핑은 항등이므로 null 로 들어오며, 이때는 간선 매핑이 곧 결과다.
     */
    private Map<String, String> compose(Map<String, String> originToCurrent, Map<String, String> currentToNext) {
        if (originToCurrent == null) {
            return new HashMap<>(currentToNext);
        }
        Map<String, String> composed = new HashMap<>();
        for (Map.Entry<String, String> entry : originToCurrent.entrySet()) {
            String next = currentToNext.get(entry.getValue());
            if (next != null) {
                composed.put(entry.getKey(), next);
            }
        }
        return composed;
    }

    private String chkptKey(String cmpnyCd, String siteCd, String chkptCd) {
        return cmpnyCd + "|" + siteCd + "|" + chkptCd;
    }

    /**
     * 점검 사진 티어별 복제(plan §4-5) — 원본 바이트를 읽어 수신 테넌트 소유 파일로 재저장한다.
     * 원본이 없거나 읽지 못하면 사진 없이 응답만 복제한다(응답 저장을 파일 때문에 실패시키지 않는다).
     *
     * @param fileType 대상 파일 유형(점검 응답=001, 불량조치=006) — 원본과 동일 유형으로 채번/저장한다
     * @return 그 티어의 신규 FILE_MGMT_CD(사진 없으면 빈 문자열 — 현행 앱 저장 규약과 동일)
     */
    private String copyPhotoForTier(String srcCmpnyCd, String srcFileMgmtCd, String dstCmpnyCd, String dstSiteCd,
            String fileType) {

        if (srcFileMgmtCd == null || srcFileMgmtCd.isBlank()) {
            return "";
        }

        try {
            ImageBytesResult image = fileService.loadImageBytes(new FileReadQuery(srcCmpnyCd, srcFileMgmtCd));
            if (image == null || image.data() == null || image.data().length == 0) {
                log.warn("점검 사진 복제 생략(원본 없음) - src={}:{}, dst={}", srcCmpnyCd, srcFileMgmtCd, dstCmpnyCd);
                return "";
            }

            String ext = EXT_BY_MEDIA_TYPE.get(image.mediaType());
            if (ext == null) {
                log.warn("점검 사진 복제 생략(지원하지 않는 형식) - src={}:{}, mediaType={}",
                        srcCmpnyCd, srcFileMgmtCd, image.mediaType());
                return "";
            }

            String newFileMgmtCd = fileMapper.selectFileMgmtCd(
                    FileInfoQuery.from(dstCmpnyCd, fileType));

            fileService.fileSave(FileInfoParam.from(
                    dstCmpnyCd
                    , "SYSTEM"
                    , dstSiteCd
                    , fileType
                    , newFileMgmtCd
                    , new BytesMultipartFile("file", newFileMgmtCd + "." + ext, image.mediaType(), image.data())));

            return newFileMgmtCd;

        } catch (Exception e) {
            // 사진 복제 실패는 응답 통합 자체를 막지 않는다(사진 없이 복제 — 실패 로그만 남긴다).
            log.error("점검 사진 복제 실패 - src={}:{}, dst={}:{}, 원인={}",
                    srcCmpnyCd, srcFileMgmtCd, dstCmpnyCd, dstSiteCd, e.getMessage());
            return "";
        }
    }

    /** 체인 해석용 BFS 노드(점검대상 좌표 + 진입 방향 직전 노드 회사 + 깊이 + 기점 문항 매핑). */
    private record BfsNode(
        String cmpnyCd
        , String siteCd
        , String chkLstType
        , String chkptCd
        , String viaCmpnyCd
        , int depth
        , Map<String, String> itemCdByOriginItemCd
    ){
    }
}
