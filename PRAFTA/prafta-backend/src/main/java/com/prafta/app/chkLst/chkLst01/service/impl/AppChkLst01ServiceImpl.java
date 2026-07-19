package com.prafta.app.chkLst.chkLst01.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.chkLst.chkLst01.application.command.InspectResultSaveCommand;
import com.prafta.app.chkLst.chkLst01.application.model.InspectAnswerItemModel;
import com.prafta.app.chkLst.chkLst01.application.param.ChecklistInfoParam;
import com.prafta.app.chkLst.chkLst01.application.param.InspectResultSaveParam;
import com.prafta.app.chkLst.chkLst01.application.query.ChecklistInfoQuery;
import com.prafta.app.chkLst.chkLst01.dto.response.ChecklistInfoResponse;
import com.prafta.app.chkLst.chkLst01.dto.response.CheckpointContextResponse;
import com.prafta.app.chkLst.chkLst01.dto.response.SaveInspectResultResponse;
import com.prafta.app.chkLst.chkLst01.mapper.AppChkLst01Mapper;
import com.prafta.app.chkLst.chkLst01.result.ChecklistInfoResult;
import com.prafta.app.chkLst.chkLst01.service.AppChkLst01Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.worktime.service.WorktimeGateService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.chkLst.ChkLstErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.application.model.ChkptAnswerChain;
import com.prafta.web.subcon.subcon02.application.param.InspectAnswerPropagateParam;
import com.prafta.web.subcon.subcon02.service.ChkptResultHistRecorder;
import com.prafta.web.subcon.subcon02.service.InspectAnswerPropagationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-036-B1: chkLst01 서비스 구현.
 * <p>prafta-app-011 변경사항:
 *   <ul>
 *     <li>siteCd 불일치(param.siteCdMismatch) -> siteNm 조회 후 403(CHKLST_403_001) 차단.</li>
 *     <li>체크포인트 미존재(결과 empty) -> 404(CHKLST_404_001).</li>
 *     <li>checklist-infos 응답에 checkpoint 컨텍스트 객체 추가.</li>
 *     <li>saveInspectResult: void -> SaveInspectResultResponse (요약 반환).</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppChkLst01ServiceImpl implements AppChkLst01Service {

    private static final Pattern FILE_KEY_PATTERN = Pattern.compile("^files\\[(.+)]$");
    private static final String FILE_TYPE_DAILY_INSPECT = "001"; // 001: 일일점검

    /**
     * [보안검토 M3] 점검답변타입(INSPECT_ANSWER_TYPE, SYS009) 화이트리스트.
     *
     * <p>컬럼은 varchar(2) NOT NULL 인데 검증이 없어, 임의 값이 타 테넌트 응답행까지 전파되거나
     * 2자 초과 입력 시 전파 트랜잭션이 500 으로 터졌다. 코드 전수 조사 결과 실제 사용값은 'Y'(양호)/'N'(불량)
     * 두 가지뿐이다(앱 SafetyInspectItem.vue 토글, chkLst03/chkLst04/acct01/safety 집계 SQL 전부 Y/N 기준).
     */
    private static final Set<String> INSPECT_VALUE_WHITELIST = Set.of("Y", "N");

    /** [보안검토 M3] 사용자 입력 서술 필드 길이 상한(타 테넌트로 전파되는 값 — 무제한 입력 차단). */
    private static final int DESC_MAX_LEN = 1000;

    private final AppChkLst01Mapper appChkLst01Mapper;
    private final ObjectMapper objectMapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    /** prafta-app-022: 안전점검 등록 근무중 게이트(근무중에만 등록 허용). */
    private final WorktimeGateService worktimeGateService;

    /** PRAFTA-SUBCON-T6-05: 점검 응답 write-through 전파(연동 없으면 no-op). */
    private final InspectAnswerPropagationService inspectAnswerPropagationService;

    /** PRAFTA-SUBCON-T6-AUDIT-02: 점검 응답 덮어쓰기 감사 이력 캡처(기점 티어 — W1). */
    private final ChkptResultHistRecorder chkptResultHistRecorder;

    @Override
    public ChecklistInfoResponse selectChkLstInfo(ChecklistInfoParam param) {

        // prafta-app-011: siteCd 불일치 시 403 차단 (userSiteName 을 응답 JSON 독립 키로 반환)
        if (param.siteCdMismatch()) {
            String userSiteNm = appChkLst01Mapper.selectSiteNm(
                    param.gvCmpnyCd()
                    , param.siteCd()
            );
            String siteLabel = StringUtils.hasText(userSiteNm)
                    ? userSiteNm
                    : param.siteCd();
            log.warn("[chkLst01] siteCd 403 차단: req={}, token={}, siteNm={}, userCd={}",
                    param.reqSiteCd(), param.siteCd(), siteLabel, param.gvUserCd());
            throw ApiException.withExtra(
                    ChkLstErrorCode.CHKLST_403_001
                    , Map.of("userSiteName", siteLabel)
            );
        }

        TokenInfo tokenInfo = param.tokenInfo();

        List<ChecklistInfoResult> results = appChkLst01Mapper.selectChkLstInfo(
                ChecklistInfoQuery.from(param)
                , tokenInfo
        );

        // prafta-app-011: 체크포인트 미존재 -> 404
        if (results == null || results.isEmpty()) {
            log.warn("[chkLst01] 체크포인트 미존재: siteCd={}, chkptCd={}, userCd={}",
                    param.siteCd(), param.chkptCd(), param.gvUserCd());
            throw new ApiException(ChkLstErrorCode.CHKLST_404_001);
        }

        // prafta-app-011: 첫 row 에서 컨텍스트 추출 (모든 row 공통값)
        ChecklistInfoResult first = results.get(0);
        CheckpointContextResponse checkpoint = CheckpointContextResponse.builder()
                .chkptNm(first.chkptNm())
                .siteNm(first.siteNm())
                .chklstType(first.chklstType())
                .chkptDesc(first.chkptDesc())
                .totalCount(results.size())
                .build();

        return ChecklistInfoResponse.builder()
                .checklistInfos(results)
                .checkpoint(checkpoint)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaveInspectResultResponse saveInspectResult(InspectResultSaveParam param) {

        // prafta-app-011: siteCd 불일치 시 403 차단 (userSiteName 을 응답 JSON 독립 키로 반환)
        if (param.siteCdMismatch()) {
            String userSiteNm = appChkLst01Mapper.selectSiteNm(
                    param.tokenInfo().gv_cmpnyCd()
                    , param.siteCd()
            );
            String siteLabel = StringUtils.hasText(userSiteNm)
                    ? userSiteNm
                    : param.siteCd();
            log.warn("[chkLst01] save siteCd 403 차단: req={}, token={}, siteNm={}, userCd={}",
                    param.reqSiteCd(), param.siteCd(), siteLabel, param.tokenInfo().gv_userCd());
            throw ApiException.withExtra(
                    ChkLstErrorCode.CHKLST_403_001
                    , Map.of("userSiteName", siteLabel)
            );
        }

        // prafta-app-022: 근무중 게이트 — siteCd 가드 통과 후, 저장 루프 진입 직전에 차단.
        //   미근무 시 WORKTIME_403_001(저장 미수행). 조회성 메서드(selectChkLstInfo)에는 미적용.
        worktimeGateService.assertWorking(param.tokenInfo());

        try {
            // 1) multipart 'items' (JSON 묶음 파일) 파싱
            List<InspectAnswerItemModel> items = parseItems(param.items());

            // 2) 'files[ITEM_CD]' 패턴으로 들어온 이미지 파일을 itemCd 기준으로 인덱싱
            Map<String, MultipartFile> fileByItemCd = indexFilesByItemCd(param.files());

            TokenInfo tokenInfo = param.tokenInfo();
            String userCd = tokenInfo.gv_userCd();

            int okCount = 0;
            int badCount = 0;
            int savedCount = 0;

            // 문항명 해석용(L4 문항 실재 검증) + 체크포인트명(화면 C 요약).
            List<ChecklistInfoResult> chkLstRows = selectChkLstRows(param);
            Map<String, String> subjByItemCd = indexSubjByItemCd(chkLstRows);

            // [qa M-3] 점검대상(chkpt) 매핑은 이 저장 요청의 전 문항에 대해 불변이다.
            //   문항 루프 안에서 체인을 재해석하면 문항당 (부모 1 + 자식 N) 링크 조회가 반복된다(N+1).
            //   저장 1회당 체인을 1번만 열고, 티어별 문항 좌표는 체인이 보유한 매핑표로 치환한다.
            ChkptAnswerChain chain = inspectAnswerPropagationService.openChain(
                    param.cmpnyCd(), param.siteCd(), param.chkptCd());

            // 3) 항목별 처리: 입력 검증 -> (이미지 있으면) 파일 저장 -> UPSERT(덮어쓰기) -> 체인 전 티어 전파
            //   [정책 변경] 후행 덮어쓰기(last-writer-wins): 선수행 우선 skip 게이팅은 제거됐다.
            //   기존 데이터가 있어도 무조건 덮어쓴다(사용자는 앱 확인 팝업에서 이미 동의함 — ChkLst.vue).
            for (InspectAnswerItemModel item : items) {

                // [보안검토 M3] 타 테넌트로 전파되는 사용자 입력이므로 진입부에서 화이트리스트/길이를 검증한다.
                // [보안검토 L4] 그 점검대상에 실재하는 활성(시행일 도래) 문항인지 확인한다
                //   — 조회 실패로 목록이 비었을 때는 검증을 건너뛴다(저장 자체를 막지 않는 기존 폴백 유지).
                validateItemInput(item, subjByItemCd);

                String fileMgmtCd = "";
                MultipartFile img = fileByItemCd.get(item.itemCd());

                if (img != null && !img.isEmpty()) {
                    // 건별 fileMgmtCd 생성
                    fileMgmtCd = fileMapper.selectFileMgmtCd(
                            FileInfoQuery.from(param.cmpnyCd(), FILE_TYPE_DAILY_INSPECT)
                    );

                    // prafta-036-C(H-3): param.siteCd() 는 Param.from 에서 token gv_siteCd 로 캐노니컬라이즈됨
                    fileService.fileSave(FileInfoParam.from(
                            param.cmpnyCd()
                            , userCd
                            , param.siteCd()
                            , FILE_TYPE_DAILY_INSPECT
                            , fileMgmtCd
                            , img
                    ));
                }

                // PRAFTA-SUBCON-T6-AUDIT-02(W1): write 직전 좌표 존재여부로 CHG_TYPE(신규/덮어쓰기)을 판정한다.
                //   기점 티어의 HIST 는 반드시 여기서 캡처한다(전파 체인은 기점을 제외하므로).
                boolean answerExisted = chkptResultHistRecorder.existsAnswer(
                        param.cmpnyCd(), param.siteCd(), param.chkptCd(), item.itemCd(), param.workDate());

                appChkLst01Mapper.mergeChkptInspectAnswer(
                        InspectResultSaveCommand.from(param, item, fileMgmtCd)
                        , tokenInfo
                );

                // PRAFTA-SUBCON-T6-AUDIT-02(W1): write 직후 방금 쓴 행을 HIST 로 append(트리거 주체=수행자 USER_CD).
                chkptResultHistRecorder.captureAnswer(
                        param.cmpnyCd(), param.siteCd(), param.chkptCd(), item.itemCd(), param.workDate(),
                        ChkptResultHistRecorder.chgType(answerExisted), userCd);

                // PRAFTA-SUBCON-T6-05: 체인 전 티어(상·하 양방향)의 대응 좌표에 응답 복제(사진 포함, 덮어쓰기).
                //   연동되지 않은(자체) 점검대상/문항이면 매핑 부재로 no-op(체인이 비어 있음).
                inspectAnswerPropagationService.propagateAnswer(chain, new InspectAnswerPropagateParam(
                        param.cmpnyCd()
                        , param.siteCd()
                        , param.chkptCd()
                        , item.itemCd()
                        , param.workDate()
                        , item.inspectValue()
                        , item.answerDesc()
                        , fileMgmtCd
                        , userCd
                        , tokenInfo.gv_userNm()
                ));

                savedCount++;

                // prafta-app-011: 양호/불량 집계(실제 저장분만)
                if ("Y".equals(item.inspectValue())) {
                    okCount++;
                } else if ("N".equals(item.inspectValue())) {
                    badCount++;
                }
            }

            // prafta-app-011: 체크포인트명 (화면 C 요약 표시용)
            String chkptName = resolveChkptName(chkLstRows);

            return SaveInspectResultResponse.builder()
                    .chkptName(chkptName)
                    .okCount(okCount)
                    .badCount(badCount)
                    .savedCount(savedCount)
                    .workDate(param.workDate())
                    .build();

        } catch (ApiException ae) {
            // 명시적 비즈니스 예외는 그대로 전파
            throw ae;
        } catch (Exception e) {
            // 기존 silent swallow 제거: 실패 시 500 으로 surface (트랜잭션 롤백 동반)
            log.error("[chkLst01] saveInspectResult 실패", e);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    /**
     * [보안검토 M3/L4] 저장 항목 입력 검증 — 이 값들은 write-through 로 <b>타 테넌트 행에 그대로 기록</b>되므로
     * 진입부에서 차단한다(전파 도중 500 으로 터지면 원본 저장까지 롤백된다).
     *
     * @param subjByItemCd 그 점검대상의 활성 문항 목록(조회 실패 시 빈 맵 — 이때는 문항 실재 검증을 생략한다)
     */
    private void validateItemInput(InspectAnswerItemModel item, Map<String, String> subjByItemCd) {

        if (item.itemCd() == null || item.itemCd().isBlank()) {
            throw new ApiException(ChkLstErrorCode.CHKLST_400_002);
        }

        // L4: 그 점검대상에 실재하는 활성 문항만 저장 허용(임의 문항코드로 유령 응답행 생성 차단).
        if (!subjByItemCd.isEmpty() && !subjByItemCd.containsKey(item.itemCd())) {
            log.warn("[chkLst01] 미존재/비활성 문항 저장 거부 - itemCd={}", item.itemCd());
            throw new ApiException(ChkLstErrorCode.CHKLST_400_002);
        }

        // M3: 점검답변타입 화이트리스트(SYS009 — 실사용 코드값 Y/N).
        if (!INSPECT_VALUE_WHITELIST.contains(item.inspectValue())) {
            log.warn("[chkLst01] 허용되지 않은 점검답변타입 - itemCd={}", item.itemCd());
            throw new ApiException(ChkLstErrorCode.CHKLST_400_002);
        }

        // M3: 답변 상세 길이 상한.
        if (item.answerDesc() != null && item.answerDesc().length() > DESC_MAX_LEN) {
            log.warn("[chkLst01] 답변 상세 길이 초과 - itemCd={}, len={}", item.itemCd(), item.answerDesc().length());
            throw new ApiException(ChkLstErrorCode.CHKLST_400_003);
        }
    }

    /**
     * 저장 플로우에서 사용할 체크리스트 행 조회(체크포인트명 + 문항명 해석용).
     * 조회 실패는 저장 자체를 막지 않는다(빈 목록 폴백 — 요약/안내 문구만 비게 된다).
     */
    private List<ChecklistInfoResult> selectChkLstRows(InspectResultSaveParam param) {
        try {
            ChecklistInfoQuery query = new ChecklistInfoQuery(
                    param.cmpnyCd()
                    , param.siteCd()
                    , param.chkptCd()
                    , null
            );
            List<ChecklistInfoResult> rows = appChkLst01Mapper.selectChkLstInfo(query, param.tokenInfo());
            return rows == null ? new ArrayList<>() : rows;
        } catch (Exception e) {
            log.warn("[chkLst01] 체크리스트 행 조회 실패 (저장은 진행): {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 저장 완료 응답에 사용할 체크포인트명(화면 C 요약 표시용). */
    private String resolveChkptName(List<ChecklistInfoResult> rows) {
        if (rows != null && !rows.isEmpty() && StringUtils.hasText(rows.get(0).chkptNm())) {
            return rows.get(0).chkptNm();
        }
        return "";
    }

    /** PRAFTA-SUBCON-T6-05: 문항코드 → 문항명(선수행 skip 안내 문구 구성용). */
    private Map<String, String> indexSubjByItemCd(List<ChecklistInfoResult> rows) {
        Map<String, String> subjByItemCd = new HashMap<>();
        if (rows == null) {
            return subjByItemCd;
        }
        for (ChecklistInfoResult row : rows) {
            if (row.inspectItemCd() != null) {
                subjByItemCd.put(row.inspectItemCd(), row.inspectItemSubj());
            }
        }
        return subjByItemCd;
    }

    /**
     * multipart 'items' (JSON 배열) 파싱.
     * @param itemsFile MultipartFile (null/empty 허용)
     * @return 파싱된 항목 모델 목록 (비어있을 수 있음)
     */
    private List<InspectAnswerItemModel> parseItems(MultipartFile itemsFile) throws java.io.IOException {

        List<InspectAnswerItemModel> result = new ArrayList<>();

        if (itemsFile == null || itemsFile.isEmpty()) {
            return result;
        }

        String itemsJson = new String(itemsFile.getBytes(), StandardCharsets.UTF_8);
        if (itemsJson.isEmpty()) {
            return result;
        }

        JsonNode node = objectMapper.readTree(itemsJson);
        if (node.isArray()) {
            for (JsonNode n : node) {
                result.add(objectMapper.treeToValue(n, InspectAnswerItemModel.class));
            }
        }
        return result;
    }

    /**
     * 'files[ITEM_CD]' 키 패턴에서 ITEM_CD 추출하여 인덱싱.
     */
    private Map<String, MultipartFile> indexFilesByItemCd(Map<String, MultipartFile> files) {

        Map<String, MultipartFile> fileByItemCd = new HashMap<>();
        if (files == null) {
            return fileByItemCd;
        }

        for (Map.Entry<String, MultipartFile> e : files.entrySet()) {
            Matcher m = FILE_KEY_PATTERN.matcher(e.getKey());
            if (m.find()) {
                fileByItemCd.put(m.group(1), e.getValue());
            }
        }
        return fileByItemCd;
    }
}
