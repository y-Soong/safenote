package com.prafta.common.cmm.dailycontract.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.dailycontract.ContractImageComposer;
import com.prafta.common.cmm.dailycontract.ContractPdfBuilder;
import com.prafta.common.cmm.dailycontract.application.command.ContractInsertCommand;
import com.prafta.common.cmm.dailycontract.application.command.ContractSignInsertCommand;
import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.mapper.DailyContractMapper;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;
import com.prafta.common.cmm.dailycontract.result.ContractAmendPrecheckResult;
import com.prafta.common.cmm.dailycontract.result.ContractDocMeta;
import com.prafta.common.cmm.dailycontract.result.ContractLockRow;
import com.prafta.common.cmm.dailycontract.result.ContractMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.result.DailyUserMetaResult;
import com.prafta.common.cmm.dailycontract.result.EntryCycleResult;
import com.prafta.common.cmm.dailycontract.result.SignGateResult;
import com.prafta.common.cmm.dailycontract.service.DailyContractService;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.file.support.ImageProbe;
import com.prafta.common.cmm.file.support.PageStackComposer;
import com.prafta.common.cmm.file.support.PdfSupport;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.error.file.FileErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일용직 근로계약서 core 서비스 구현.
 *
 * <p>파일 저장/로드는 공통 {@code FileService}(file.upload.base-dir 외부화 경로 + tb_file_info 적재 +
 * 확장자 화이트리스트/traversal 방어)를 재사용한다. 파일 경로는 응답에 노출하지 않는다(스트림만).
 *
 * <p><b>서명·열람 대상 계약서 = 승인 시점 확정 버전(pin)</b>(승인시점 버전확정 T2 / K1). 활성 버전이 아니다.
 * 게이트 판정/열람/합성 5경로가 모두 {@link #resolveEffectiveContract}를 경유하므로 "근로자가 읽은 문서"와
 * "서명본으로 합성된 문서"가 갈라질 수 없다(선행 qa M-2 / security SEC-4 해소).
 *
 * <p>게이트 판정(D8): 대상 계약서 없음 → 스킵 / 대상 버전 서명 없음 → 필요 /
 * 서명의 REQ_ID 가 현재 승인 사이클 REQ_ID 와 불일치 → 필요(재입장 재서명) / REQ_ID NULL 레거시는 버전만 비교.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyContractServiceImpl implements DailyContractService {

    private final DailyContractMapper dailyContractMapper;
    private final DailyEntryService dailyEntryService;
    private final FileService fileService;
    private final FileMapper fileMapper;

    /** TB_FILE_INFO.FILE_TYPE — 007: 일용직계약서(양식/서명 원본/합성본 공용, SYS010). */
    private static final String FILE_TYPE_DAILY_CONTRACT = "007";

    /** 관리자 계약서 업로드 크기 상한(10MB — 요청서 §5-2/에러 400_004 문구 정합). */
    private static final long CONTRACT_FILE_MAX_BYTES = 10L * 1024 * 1024;

    /** 서명 PNG 업로드 크기 상한(5MB — TBM 종료 서명 SIGN_FILE_MAX_BYTES 미러). */
    private static final long SIGN_FILE_MAX_BYTES = 5L * 1024 * 1024;

    /** 계약서 양식 페이지 수 상한(멀티페이지 지원 P5 — 온디맨드 렌더 비용/자원 고갈 상한). */
    private static final int CONTRACT_MAX_PAGES = 20;

    /** 계약서 양식 허용 확장자(점 제외 소문자) — 업로드 정합 검증 + 열람 시 도메인 재검증 공용. */
    private static final Set<String> CONTRACT_ALLOWED_EXTS = Set.of("png", "jpg", "jpeg", "pdf");

    /** PDF 양식 확장자(점 제외 소문자). */
    private static final String EXT_PDF = "pdf";

    /** 계약서 형식 코드 — 응답 formatType. */
    private static final String FORMAT_PDF = "PDF";
    private static final String FORMAT_IMG = "IMG";

    /** 열람 페이지 렌더 DPI(P6 — 폰 확대 가독성). */
    private static final float PAGE_RENDER_DPI = 150f;

    /** 구버전 앱 폴백 병합 렌더 DPI(D-P8). */
    private static final float FALLBACK_MERGE_DPI = 100f;

    /** 구버전 앱 폴백 병합 PNG 총 픽셀 상한(D-P8) — OOM 하드 가드. */
    private static final long FALLBACK_MERGE_MAX_PIXELS = 40_000_000L;

    /**
     * PDF 양식 업로드 허용 최대 렌더 픽셀 수 — <b>페이지 1장 기준</b>(sec SEC-1).
     *
     * <p>{@link #PAGE_RENDER_DPI}(150) 로 렌더할 때 A3(297×420mm ≈ 1,050만 px)를 수용하고
     * 그 이상은 차단한다. 바이트 크기·페이지 수만 검증하면 "수십 KB / 20페이지 /
     * 페이지당 14400×14400pt(PDF 스펙 상한)" 이 통과하는데, 렌더 시 페이지당 9억 픽셀
     * (TYPE_INT_RGB 3.6GB)이 되어 OOM 이 난다. OOM 은 {@code Error} 라 전역 핸들러가 잡지 못하고
     * 인스턴스 전체(전 테넌트)로 파급되므로 등록 시점에 차단한다.
     */
    private static final long CONTRACT_MAX_PAGE_RENDER_PIXELS = 12_000_000L;

    /**
     * 이미지 양식 업로드 허용 최대 픽셀 수(sec SEC-1).
     *
     * <p>이미지는 DPI 변환 없이 원본 픽셀이 그대로 디코딩되므로 별도 상한을 둔다. 1200dpi 스캔
     * A4 PNG(9920×14030 = 1.39억 px)는 흰 배경이면 10MB 이내로 압축되어 바이트 상한을 통과하지만
     * 디코딩만으로 557MB 를 요구한다. 상한은 "세로로 이어붙인 수 페이지 이미지"(기존 우회 방식)를
     * 계속 수용할 수 있도록 여유를 둔 값이다 — A4 300dpi 5장 연결(약 1,740만 px)도 통과한다.
     */
    private static final long CONTRACT_MAX_IMAGE_PIXELS = 50_000_000L;

    /** 계약서명 최대 길이(DDL varchar(200) 정합). */
    private static final int CONTRACT_NM_MAX_LEN = 200;

    /** 목록 조회 상한(전수조회 방지). */
    private static final int LIST_LIMIT = 500;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISP_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISP_DTIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ActiveContractResult findActiveContract(String cmpnyCd, String siteCd) {
        return dailyContractMapper.selectActiveContract(cmpnyCd, siteCd);
    }

    @Override
    public SignGateResult judgeSignGate(String cmpnyCd, String userCd) {
        // 1) 일용직 메타(사업장) — TB_DAILY_USER 미존재(정규직 등)면 게이트 대상 아님.
        DailyUserMetaResult meta = dailyContractMapper.selectDailyUserMeta(cmpnyCd, userCd);
        if (meta == null) {
            return SignGateResult.skip();
        }

        // 2) 서명 대상 계약서 결정(pin 우선 — J4). null 이면 스킵:
        //      · pin=0  → 승인 시점 계약서 미등록(K4)
        //      · pin NULL + 활성도 없음 → 미등록 사업장(R2 — 필수약관 "미등록=스킵" 패턴 미러)
        EffectiveContract eff = resolveEffectiveContract(cmpnyCd, meta.siteCd(), userCd);
        if (eff == null) {
            return SignGateResult.skip();
        }

        // 3) 대상 버전 서명 존재 + 사이클 일치 여부(D8-①·②).
        if (isCurrentCycleSigned(cmpnyCd, userCd, eff)) {
            return SignGateResult.skip();
        }
        return SignGateResult.required(eff.contractVer(), eff.contractNm());
    }

    /**
     * 서명·열람 대상 계약서 (승인시점 버전확정 T2 / §1 단일 리졸버).
     *
     * <p>게이트·열람·합성이 각자 계약서를 결정하면 "읽은 문서 != 서명한 문서"가 성립하므로
     * 결정 지점을 이 메서드 하나로 묶는다. {@code reqId} 를 함께 담아
     * {@link #isCurrentCycleSigned}·{@code insertContractSign} 이 <b>같은 행</b>을 근거로 쓰게 한다.
     *
     * @param contractVer 대상 계약서 버전
     * @param contractNm  대상 계약서명(게이트 응답/메타 응답용)
     * @param fileMgmtCd  대상 원본 파일코드(응답에는 절대 노출하지 않는다 — 스트림 전용)
     * @param reqId       현재 승인 사이클 REQ_ID(없으면 null — 레거시 계정)
     */
    private record EffectiveContract(int contractVer, String contractNm, String fileMgmtCd, String reqId) {
    }

    /**
     * 서명·열람 대상 계약서 결정 — pin 우선, 레거시는 활성 폴백(승인시점 버전확정 §1).
     *
     * <p>pin 3상태 분기:
     * <ul>
     *   <li>{@code >0} : 그 버전(<b>USE_YN 무관</b> — K5. 사용중지되어도 완주)</li>
     *   <li>{@code 0}  : {@code null} 반환 = 그 사이클은 게이트 스킵(K4 — 승인 시점 미등록.
     *       이후 관리자가 등록해도 그 사이클에는 적용하지 않고 다음 승인 사이클부터 적용)</li>
     *   <li>{@code NULL}: 배포 전 레거시 승인 → 활성 계약서 폴백(K8). 활성도 없으면 {@code null}</li>
     * </ul>
     *
     * <p>pin 버전 행이 소실된 예외 상황(수동 DELETE 등)은 경고 로그 + 활성 폴백으로 강등한다(방어).
     *
     * @return 대상 계약서. 게이트 스킵/미등록이면 null
     */
    private EffectiveContract resolveEffectiveContract(String cmpnyCd, String siteCd, String userCd) {
        EntryCycleResult cycle = dailyContractMapper.selectCurrentCycleRequest(cmpnyCd, userCd);
        Integer pinnedVer = cycle == null ? null : cycle.contractVer();
        String cycleReqId = cycle == null ? null : cycle.reqId();

        if (pinnedVer != null) {
            if (pinnedVer == 0) {
                // K4 — 승인 시점에 계약서가 없었다. 활성 폴백을 태우면 K4 를 위반한다.
                log.info("승인 시점 계약서 미등록(pin=0) — 서명 게이트 스킵. cmpnyCd={}, userCd={}, reqId={}",
                        cmpnyCd, userCd, cycleReqId);
                return null;
            }
            ActiveContractResult pinned = dailyContractMapper.selectContractByVer(cmpnyCd, siteCd, pinnedVer);
            if (pinned != null) {
                return new EffectiveContract(
                        pinned.contractVer(), pinned.contractNm(), pinned.fileMgmtCd(), cycleReqId);
            }
            // 확정 버전 행이 사라진 비정상 상태 — 서명을 완전히 막기보다 활성 기준으로 강등한다.
            log.warn("승인 시점 확정 계약서 버전 행 없음(활성 기준 폴백) — cmpnyCd={}, siteCd={}, pinVer={}, reqId={}",
                    cmpnyCd, siteCd, pinnedVer, cycleReqId);
        }

        // K8 — 배포 전 레거시 승인(pin NULL) 또는 사이클 행 자체가 없는 계정: 기존 동작(활성 기준) 유지.
        ActiveContractResult active = dailyContractMapper.selectActiveContract(cmpnyCd, siteCd);
        if (active == null) {
            return null;
        }
        return new EffectiveContract(
                active.contractVer(), active.contractNm(), active.fileMgmtCd(), cycleReqId);
    }

    /**
     * 대상 버전·현재 승인 사이클 서명 완료 여부 (게이트 판정/서명 멱등 가드 공용, 승인시점 버전확정 §3).
     *
     * <p>판정: 대상 버전 서명 존재 AND (서명 REQ_ID == 현재 사이클 REQ_ID
     * OR 서명 REQ_ID IS NULL(레거시 — 버전만 비교) OR 현재 사이클 REQ_ID 자체가 없음(배포 전 활성 계정)).
     *
     * <p>변경점: 비교 버전이 "활성"에서 "{@code eff.contractVer()}(pin 우선)"로 바뀌었고, 사이클 REQ_ID 를
     * 메서드 안에서 재조회하지 않고 <b>리졸버가 pin 을 읽은 그 행</b>의 값을 받는다(판정 출처 단일화).
     * 레거시 폴백 2분기(REQ_ID NULL / 사이클 없음)는 문장 그대로 유지 = 무회귀.
     */
    private boolean isCurrentCycleSigned(String cmpnyCd, String userCd, EffectiveContract eff) {
        ContractSignMetaResult latestSign =
                dailyContractMapper.selectLatestSignForVer(cmpnyCd, userCd, eff.contractVer());
        if (latestSign == null) {
            return false;
        }
        if (latestSign.reqId() == null || latestSign.reqId().isBlank()) {
            // 레거시 서명(승인 사이클 미기록) — 버전 일치만으로 서명 완료 취급.
            return true;
        }
        if (eff.reqId() == null || eff.reqId().isBlank()) {
            // 현재 사이클 식별 불가(소진·승인 요청 없음 = 배포 전 기존 활성 계정 등) — 버전 일치만으로 판정.
            return true;
        }
        // 서명 사이클 == 현재 승인 사이클이면 완료, 다르면 재입장 재서명 필요(D8-①).
        return eff.reqId().equals(latestSign.reqId());
    }

    @Override
    @Transactional
    public int registerContract(String cmpnyCd, String siteCd, String contractNm, MultipartFile file,
            String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank() || contractNm == null || contractNm.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (contractNm.length() > CONTRACT_NM_MAX_LEN) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        // 업로드 검증 — PDF/이미지(png/jpg) + 10MB 이하 + 확장자-내용 정합 + 실디코딩/실파싱 성공.
        //   실검증 유지 근거(SEC-2): contentType 은 클라이언트 신고값이라 스푸핑 가능. 디코딩/파싱 불가 파일이
        //   활성 계약서로 등록되면 해당 사업장 전체 일용직의 서명 합성이 전면 실패하므로 업로드 시점에 차단한다.
        validateContractUpload(file);

        // 기존 활성 'N' → 버전 MAX+1 INSERT (한 트랜잭션 — 동시 등록은 기능성 유니크가 백스톱).
        dailyContractMapper.updateContractDeactivate(cmpnyCd, siteCd, procUserCd);
        int nextVer = dailyContractMapper.selectNextContractVer(cmpnyCd, siteCd);

        String fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(cmpnyCd, FILE_TYPE_DAILY_CONTRACT));
        fileService.fileSave(FileInfoParam.from(
                cmpnyCd, procUserCd, siteCd, FILE_TYPE_DAILY_CONTRACT, fileMgmtCd, file));

        try {
            dailyContractMapper.insertContract(new ContractInsertCommand(
                    cmpnyCd, siteCd, nextVer, contractNm, fileMgmtCd, procUserCd));
        } catch (DuplicateKeyException e) {
            // 최초 등록(활성 0건) 동시성(qa L-1) — deactivate 0행이면 행 잠금이 없어 selectNextContractVer
            // 경합 시 후행 INSERT 가 PK/기능성 유니크(UX_DAILY_CONTRACT_ACTIVE) 충돌한다.
            // 빈 범위 FOR UPDATE 는 gap 락 상호 호환으로 직렬화가 성립하지 않으므로(교착 유발),
            // dailyentry 의 DuplicateKeyException 캐치 스타일을 미러해 명시 에러로 매핑(재시도 안내).
            // 활성 존재 시에는 deactivate 행 잠금이 직렬화하는 기존 동작 그대로 유지된다.
            log.info("일용직 계약서 동시 등록 경합 감지 — cmpnyCd={}, siteCd={}, ver={}", cmpnyCd, siteCd, nextVer);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_409_001);
        }

        log.info("일용직 계약서 등록 완료 — cmpnyCd={}, siteCd={}, ver={}, 등록자={}",
                cmpnyCd, siteCd, nextVer, procUserCd);
        return nextVer;
    }

    @Override
    @Transactional
    public void stopContract(String cmpnyCd, String siteCd, String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        int updated = dailyContractMapper.updateContractDeactivate(cmpnyCd, siteCd, procUserCd);
        if (updated <= 0) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }
        log.info("일용직 계약서 사용중지 완료 — cmpnyCd={}, siteCd={}, 처리자={}", cmpnyCd, siteCd, procUserCd);
    }

    @Override
    @Transactional
    public void amendActiveContract(String cmpnyCd, String siteCd, int contractVer, MultipartFile file,
            String procUserCd, String procAuthCd) {
        // 1) 필수값 검증. 계약서명은 정정 대상이 아니므로 파라미터 자체를 받지 않는다(Q5).
        if (siteCd == null || siteCd.isBlank() || contractVer < 1) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 2) 사업장 인가 가드 — 등록/중지와 동일 기준. ★파일 저장·UPDATE 보다 반드시 먼저(IDOR).
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        // 3) 업로드 검증 — ★등록과 동일 메서드 재사용(J11). 정정으로 검증을 우회할 수 없다.
        //    잠금 '앞'에서 수행한다(qa Q-6): PDF 파싱·픽셀 산출은 비용이 크므로 잠금 안에서 돌리면
        //    계약서 행 잠금 점유가 길어져 동시 정정/교체가 불필요하게 대기한다(등록 경로도 잠금 앞 검증).
        validateContractUpload(file);

        // 4) 대상 행 잠금 조회(FOR UPDATE) — 동시 정정 / 정정+교체를 직렬화한다.
        //    미존재(타 회사·타 사업장 버전 포함)는 400_003 으로 통일(존재 비노출).
        ContractLockRow target = dailyContractMapper.selectContractForUpdate(cmpnyCd, siteCd, contractVer);
        if (target == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }
        if (!"Y".equals(target.useYn())) {
            // 그 사이 다른 관리자가 교체/중지 → 정정 대상이 아니다.
            log.info("일용직 계약서 정정 차단(활성 아님) — cmpnyCd={}, siteCd={}, ver={}, useYn={}",
                    cmpnyCd, siteCd, contractVer, target.useYn());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_409_002);
        }

        // 5) 서명 0건 검증 — ★서버측 강제(프론트 버튼 숨김과 무관, K6).
        //    잠금 뒤에 세는 이유: 잠금 없이 세면 "정정 중 서명 커밋" 경합에서 판정이 뒤집힌다.
        //    (단 signContract 는 계약서 행을 잠그지 않으므로 완전 직렬화는 아니다 — plan §7 R3, security 판정 항목)
        int signCnt = dailyContractMapper.selectSignCntByVer(cmpnyCd, siteCd, contractVer);
        if (signCnt > 0) {
            log.info("일용직 계약서 정정 차단(서명자 존재) — cmpnyCd={}, siteCd={}, ver={}, signCnt={}",
                    cmpnyCd, siteCd, contractVer, signCnt);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_009);
        }

        // 6) 새 파일 저장. 기존 파일 행·디스크 파일은 삭제하지 않는다(J8).
        String newFileMgmtCd = saveContractFile(cmpnyCd, procUserCd, siteCd, file);

        // 7) 참조 교체 — CONTRACT_VER 미증가 / USE_YN 불변 / CONTRACT_NM 불변(J9·Q5).
        //    USE_YN='Y' 조건부 UPDATE 가 잠금 이후 상태 반전에 대한 백스톱이다.
        int updated = dailyContractMapper.updateContractFile(
                cmpnyCd, siteCd, contractVer, newFileMgmtCd, procUserCd);
        if (updated <= 0) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_409_002);
        }

        // 8) ★이전 FILE_MGMT_CD 를 로그에 남긴다 — DB 에는 남지 않으므로 이것이 유일한 감사 흔적이다(J8).
        log.info("일용직 계약서 정정 완료 — cmpnyCd={}, siteCd={}, ver={}, 이전파일={}, 새파일={}, 처리자={}",
                cmpnyCd, siteCd, contractVer, target.fileMgmtCd(), newFileMgmtCd, procUserCd);
    }

    @Override
    public ContractAmendPrecheckResult findAmendPrecheck(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank() || contractVer < 1) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        // 읽기 전용 EP 라 FOR UPDATE 를 쓰지 않는다(조회만으로 행 잠금이 생기면 안 된다).
        //   최종 방어는 amendActiveContract 의 잠금 + 조건부 UPDATE 다.
        if (dailyContractMapper.selectContractByVer(cmpnyCd, siteCd, contractVer) == null) {
            return null;
        }

        int signCnt = dailyContractMapper.selectSignCntByVer(cmpnyCd, siteCd, contractVer);
        // J10 경고 데이터 — 두 값을 분리한다. pin 은 승인 시점에만 기록되므로 대기('01')에는 버전 정보가 없다.
        int pinnedApprovedCnt = dailyContractMapper.selectPinnedApprovedReqCnt(cmpnyCd, siteCd, contractVer);
        int pendingCnt = dailyContractMapper.selectPendingReqCnt(cmpnyCd, siteCd);

        return new ContractAmendPrecheckResult(signCnt == 0, signCnt, pinnedApprovedCnt, pendingCnt);
    }

    @Override
    public List<ContractVersionRow> findContractVersions(String cmpnyCd, String siteCd,
            String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        return dailyContractMapper.selectContractVersionList(cmpnyCd, siteCd, LIST_LIMIT);
    }

    @Override
    public FileBytesResult loadContractImageForAdmin(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        String fileMgmtCd = dailyContractMapper.selectContractFileMgmtCd(cmpnyCd, siteCd, contractVer);
        if (fileMgmtCd == null) {
            // 해당 버전 자체가 없음 → 호출부가 400_003(등록된 계약서가 없습니다)으로 매핑.
            return null;
        }

        FileBytesResult image = loadContractFileOrNull(cmpnyCd, fileMgmtCd);
        if (image == null) {
            // 계약서 행은 있는데 디스크 원본이 없는 상태(DB 만 이관되고 업로드 파일 미이관 등).
            //   "미등록"과 구분해야 운영자가 재등록으로 바로 복구할 수 있다.
            log.error("계약서 원본 파일 없음 — cmpnyCd={}, siteCd={}, ver={}, fileMgmtCd={}",
                    cmpnyCd, siteCd, contractVer, fileMgmtCd);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }
        return image;
    }

    @Override
    public FileBytesResult loadActiveContractImageForUser(String cmpnyCd, String userCd) {
        DailyUserMetaResult meta = dailyContractMapper.selectDailyUserMeta(cmpnyCd, userCd);
        if (meta == null) {
            return null;
        }
        // 승인 시점 확정(pin) 기준 — 구버전 앱이 활성본을 읽고 pin 본에 서명하면 M-2 가 재현된다(J5).
        EffectiveContract eff = resolveEffectiveContract(cmpnyCd, meta.siteCd(), userCd);
        if (eff == null) {
            return null;
        }

        FileBytesResult image = loadContractFileOrNull(cmpnyCd, eff.fileMgmtCd());
        if (image == null) {
            // 계약서 행은 있는데 원본 파일이 없음 — 일용직에게 "미등록"으로 안내되면 안 된다(관리자 재등록 필요).
            log.error("일용직 계약서 원본 파일 없음 — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), eff.contractVer());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }
        // 구버전 앱 폴백(T5/P7): 원본이 PDF 면 전 페이지 세로 병합 PNG 로 내려 준다(URL·인가·응답 형태 무변경).
        return toLegacyViewableImage(image);
    }

    @Override
    @Transactional
    public ContractSignMetaResult signContract(String cmpnyCd, String userCd, MultipartFile signFile) {
        // 1) 서명 이미지 서버 검증(크기/형식) + PNG 디코딩(polyglot 차단, 합성 재사용).
        validateImageUpload(signFile, SIGN_FILE_MAX_BYTES, DailyContractErrorCode.DAILYCONTRACT_400_001);
        BufferedImage signImage = decodeImageOrThrow(signFile, DailyContractErrorCode.DAILYCONTRACT_400_001);

        // 2) 본인(일용직) 메타 — 사업장/이름 스냅샷의 서버측 단일 출처.
        DailyUserMetaResult meta = dailyContractMapper.selectDailyUserMeta(cmpnyCd, userCd);
        if (meta == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // 3) 서명 대상 계약서 필수 — ★활성이 아니라 승인 시점 확정(pin) 버전(K1).
        //    열람 EP(contract-meta/contract-page/contract-image)와 같은 리졸버를 타므로
        //    "근로자가 읽은 문서"와 "합성 대상 문서"가 구조적으로 같다(M-2/SEC-4 해소).
        //    pin=0(승인 시점 미등록)도 여기서는 400_003 — 서명할 대상 자체가 없다.
        EffectiveContract eff = resolveEffectiveContract(cmpnyCd, meta.siteCd(), userCd);
        if (eff == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        // 4) 멱등 가드 — 대상 버전·현재 사이클 서명 존재 시 400_002.
        if (isCurrentCycleSigned(cmpnyCd, userCd, eff)) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_002);
        }

        // 5) 계약서 원본 로드(경로 방어/화이트리스트는 FileService 가 수행 + 도메인 확장자 재검증).
        FileBytesResult contractFile = loadContractFileOrNull(cmpnyCd, eff.fileMgmtCd());
        if (contractFile == null) {
            log.error("일용직 계약서 원본 파일 없음(서명 불가) — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), eff.contractVer());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }

        // 6) 서명본 PDF 조립(P1·P2) — 서명 블록 PNG 렌더 + 원본 페이지 보존 + 블록 페이지 append.
        //    서명일시/최초 근로일은 서버 시각만(§6-3, D1).
        LocalDateTime now = LocalDateTime.now();
        String firstWorkDate = now.toLocalDate().format(YMD);
        byte[] mergedBytes;
        try {
            String contractExt = contractFile.fileExt();
            float pageWidthPt = ContractPdfBuilder.resolveSignBlockPageWidthPt(contractFile.data(), contractExt);
            byte[] signBlockPng = ContractImageComposer.renderSignBlock(
                    ContractPdfBuilder.resolveSignBlockRenderWidthPx(pageWidthPt)
                    , signImage
                    , meta.userNm()
                    , now.toLocalDate().format(DISP_DATE)
                    , now.format(DISP_DTIME));
            mergedBytes = ContractPdfBuilder.buildSignedPdf(contractFile.data(), contractExt, signBlockPng);
        } catch (ApiException ae) {
            // 암호 PDF 등 공통 파일 계층 예외 → 계약서 맥락 코드로 remap(조립 실패는 400_001).
            log.error("일용직 계약서 서명본 조립 실패(파일 계층) — cmpnyCd={}, siteCd={}, ver={}, code={}",
                    cmpnyCd, meta.siteCd(), eff.contractVer(), ae.getErrorCode().code());
            throw remapFileError(ae, DailyContractErrorCode.DAILYCONTRACT_400_001);
        } catch (Exception e) {
            log.error("일용직 계약서 합성 실패 — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), eff.contractVer(), e);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_001);
        }

        // 7) SHA-256(최종 서명본 PDF 바이트) — 증적 무결성(§6-3).
        String mergedSha256 = sha256Hex(mergedBytes);

        // 8) 파일 저장 — 서명 PNG 원본 + 서명본 PDF(둘 다 tb_file_info 적재, FILE_UPLOAD 외부화 경로).
        //    ★BytesMultipartFile 의 originalFilename 확장자가 저장 확장자(FILE_EXT)를 결정한다 → .pdf 고정.
        String signFileMgmtCd = saveContractFile(cmpnyCd, userCd, meta.siteCd(), signFile);
        String mergedFileMgmtCd = saveContractFile(cmpnyCd, userCd, meta.siteCd(),
                new BytesMultipartFile("file", "contract-merged.pdf", "application/pdf", mergedBytes));

        // 9) append-only INSERT — 버전/사이클 모두 리졸버가 결정한 값을 그대로 기록한다.
        //    ★재조회 금지: 게이트 판정과 기록이 다른 조회를 하면 그 사이에 상태가 바뀌는 창이 생긴다.
        //    승인 레코드의 CONTRACT_VER == 서명 레코드의 CONTRACT_VER 가 구조적으로 보장된다.
        //    reqId 는 레거시(사이클 행 없음)면 NULL.
        String reqId = eff.reqId();
        String signId = dailyContractMapper.selectSignId(cmpnyCd);
        dailyContractMapper.insertContractSign(new ContractSignInsertCommand(
                cmpnyCd
                , signId
                , meta.siteCd()
                , userCd
                , meta.userNm()
                , eff.contractVer()
                , reqId
                , signFileMgmtCd
                , mergedFileMgmtCd
                , mergedSha256
                , firstWorkDate));

        log.info("일용직 계약서 서명 완료 — cmpnyCd={}, userCd={}, signId={}, ver={}, reqId={}",
                cmpnyCd, userCd, signId, eff.contractVer(), reqId);

        return dailyContractMapper.selectSignMetaById(cmpnyCd, signId);
    }

    @Override
    public ContractSignMetaResult findMySign(String cmpnyCd, String userCd) {
        return dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
    }

    @Override
    public FileBytesResult loadMySignImage(String cmpnyCd, String userCd) {
        // 본인 스코프 강제 — (cmpnyCd, userCd)로만 조회하므로 타인 서명본 접근 불가(IDOR 차단).
        ContractSignMetaResult mySign = dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
        if (mySign == null) {
            return null;
        }
        FileBytesResult file = loadContractFileOrNull(cmpnyCd, mySign.mergedFileMgmtCd());
        if (file == null) {
            return null;
        }
        // 구버전 앱 폴백(T5/P7 + plan §2 A-5): 서명본이 PDF 면 세로 병합 PNG,
        //   레거시 PNG 합성본은 바이트 그대로(재합성·변환 금지 — P3).
        return toLegacyViewableImage(file);
    }

    @Override
    public FileBytesResult loadMySignFile(String cmpnyCd, String userCd) {
        // 본인 스코프 강제(IDOR 차단). 폴백 변환 없이 원본 바이트를 그대로 반환(신규 앱 저장/다운로드용).
        ContractSignMetaResult mySign = dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
        if (mySign == null) {
            return null;
        }
        return loadContractFileOrNull(cmpnyCd, mySign.mergedFileMgmtCd());
    }

    @Override
    public List<ContractSignRow> findSignListForAdmin(ContractSignListQuery query,
            String procUserCd, String procAuthCd) {
        if (query.siteCd() == null || query.siteCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(query.cmpnyCd(), procUserCd, procAuthCd, query.siteCd());

        return dailyContractMapper.selectSignListForAdmin(query);
    }

    @Override
    public FileBytesResult loadSignImageForAdmin(String cmpnyCd, String signId,
            String procUserCd, String procAuthCd) {
        if (signId == null || signId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 미존재(타 회사 signId 포함)는 404 — 존재 비노출.
        ContractSignMetaResult sign = dailyContractMapper.selectSignMetaById(cmpnyCd, signId);
        if (sign == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_001);
        }
        // 서명본의 SITE_CD 기준 사업장 인가 재검증(IDOR — signId 열거 차단).
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, sign.siteCd());

        // 웹은 Content-Type 동적 스트림이라 PDF/레거시 PNG 모두 원본 바이트를 그대로 내려 준다(P3).
        return loadContractFileOrNull(cmpnyCd, sign.mergedFileMgmtCd());
    }

    @Override
    public ContractMetaResult findActiveContractMeta(String cmpnyCd, String userCd) {
        DailyUserMetaResult meta = dailyContractMapper.selectDailyUserMeta(cmpnyCd, userCd);
        if (meta == null) {
            return null;
        }
        // 열람 메타도 pin 기준(J5) — 서명 대상과 다른 버전을 노출하면 M-2 가 재현된다.
        EffectiveContract eff = resolveEffectiveContract(cmpnyCd, meta.siteCd(), userCd);
        if (eff == null) {
            return null;
        }

        FileBytesResult file = loadContractFileOrNull(cmpnyCd, eff.fileMgmtCd());
        if (file == null) {
            log.error("일용직 계약서 원본 파일 없음(메타 조회) — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), eff.contractVer());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }
        ContractDocMeta doc = describeDoc(file);
        return new ContractMetaResult(eff.contractVer(), eff.contractNm(), doc.formatType(), doc.pageCount());
    }

    @Override
    public FileBytesResult loadActiveContractPage(String cmpnyCd, String userCd, int page) {
        DailyUserMetaResult meta = dailyContractMapper.selectDailyUserMeta(cmpnyCd, userCd);
        if (meta == null) {
            return null;
        }
        // 열람 페이지도 pin 기준(J5) — meta 와 같은 리졸버라 pageCount 와 실제 렌더 대상이 일치한다.
        EffectiveContract eff = resolveEffectiveContract(cmpnyCd, meta.siteCd(), userCd);
        if (eff == null) {
            return null;
        }

        FileBytesResult file = loadContractFileOrNull(cmpnyCd, eff.fileMgmtCd());
        if (file == null) {
            log.error("일용직 계약서 원본 파일 없음(페이지 조회) — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), eff.contractVer());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }
        return renderContractPage(file, page);
    }

    @Override
    public ContractDocMeta describeMySignDoc(String cmpnyCd, String userCd) {
        // 본인 스코프 강제(IDOR 차단).
        ContractSignMetaResult mySign = dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
        if (mySign == null) {
            return null;
        }
        FileBytesResult file = loadContractFileOrNull(cmpnyCd, mySign.mergedFileMgmtCd());
        if (file == null) {
            // 서명 메타는 있으나 파일 유실 — 메타 카드는 표시하되 페이지 렌더는 불가.
            log.error("서명본 파일 없음(메타 조회) — cmpnyCd={}, signId={}", cmpnyCd, mySign.signId());
            return null;
        }
        return describeDoc(file);
    }

    @Override
    public FileBytesResult loadMySignPage(String cmpnyCd, String userCd, int page) {
        // 본인 스코프 강제(IDOR 차단).
        ContractSignMetaResult mySign = dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
        if (mySign == null) {
            return null;
        }
        FileBytesResult file = loadContractFileOrNull(cmpnyCd, mySign.mergedFileMgmtCd());
        if (file == null) {
            return null;
        }
        return renderContractPage(file, page);
    }

    @Override
    public ContractDocMeta findContractMetaForAdmin(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        String fileMgmtCd = dailyContractMapper.selectContractFileMgmtCd(cmpnyCd, siteCd, contractVer);
        if (fileMgmtCd == null) {
            // 해당 버전 자체가 없음 → 호출부가 400_003 으로 매핑(존재 비노출).
            return null;
        }
        FileBytesResult file = loadContractFileOrNull(cmpnyCd, fileMgmtCd);
        if (file == null) {
            log.error("계약서 원본 파일 없음(메타 조회) — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, siteCd, contractVer);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }
        return describeDoc(file);
    }

    /**
     * 계약서 도메인 파일 로드 — 공통 로더(경로 traversal 방어/업로드 화이트리스트) + <b>도메인 확장자 좁히기</b>.
     *
     * <p>{@code loadFileBytes} 는 업로드 허용 확장자 전체를 통과시키므로, 계약서(FILE_TYPE 007)는
     * png/jpg/jpeg/pdf 로 한 번 더 좁힌다. 대상 없음(DB 행/디스크 파일 부재)은 {@code null}.
     */
    private FileBytesResult loadContractFileOrNull(String cmpnyCd, String fileMgmtCd) {
        FileBytesResult file = fileService.loadFileBytes(new FileReadQuery(cmpnyCd, fileMgmtCd));
        if (file == null) {
            return null;
        }
        String ext = file.fileExt() == null ? "" : file.fileExt().toLowerCase();
        if (!CONTRACT_ALLOWED_EXTS.contains(ext)) {
            log.error("계약서 파일 형식이 도메인 허용 범위를 벗어남 — cmpnyCd={}, ext={}", cmpnyCd, ext);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_500_001);
        }
        return file;
    }

    /** 계약서 파일의 형식/페이지 수 도출 — 이미지는 (IMG, 1), PDF 는 (PDF, 실 페이지 수). */
    private ContractDocMeta describeDoc(FileBytesResult file) {
        if (!EXT_PDF.equals(file.fileExt())) {
            return new ContractDocMeta(FORMAT_IMG, 1);
        }
        try {
            return new ContractDocMeta(FORMAT_PDF, PdfSupport.readPageCount(file.data()));
        } catch (ApiException ae) {
            throw remapFileError(ae, DailyContractErrorCode.DAILYCONTRACT_500_001);
        }
    }

    /**
     * 계약서 페이지 단건 렌더 (앱 pager — P6 150DPI).
     *
     * <p>이미지 원본/레거시 PNG 는 {@code page=1} 만 유효하며 <b>원본 바이트를 그대로</b> 반환한다(재인코딩 금지).
     * PDF 는 1-base 페이지를 PNG 로 온디맨드 렌더한다. 범위 밖 페이지는 400_007.
     */
    private FileBytesResult renderContractPage(FileBytesResult file, int page) {
        if (!EXT_PDF.equals(file.fileExt())) {
            if (page != 1) {
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
            }
            return file;
        }
        try {
            int pageCount = PdfSupport.readPageCount(file.data());
            if (page < 1 || page > pageCount) {
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
            }
            byte[] png = PdfSupport.renderPageToPng(file.data(), page - 1, PAGE_RENDER_DPI);
            if (png == null) {
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
            }
            return new FileBytesResult(png, "image/png", "png", file.fileNm());
        } catch (ApiException ae) {
            throw remapFileError(ae, DailyContractErrorCode.DAILYCONTRACT_500_001);
        }
    }

    /**
     * 구버전 앱 폴백(T5/P7) — PDF 는 전 페이지를 세로로 이어붙인 단일 PNG 로 변환한다.
     *
     * <p>이미지 원본/레거시 PNG 합성본은 <b>바이트 동일</b>로 통과시킨다(재합성·변환 금지 — P3).
     * 병합은 DPI 100 + 총 픽셀 상한 4천만(D-P8) 하드 가드로 힙 압박을 제한한다.
     */
    private FileBytesResult toLegacyViewableImage(FileBytesResult file) {
        if (!EXT_PDF.equals(file.fileExt())) {
            return file;
        }
        try {
            List<byte[]> pages = PdfSupport.renderAllPagesToPng(
                    file.data(), FALLBACK_MERGE_DPI, CONTRACT_MAX_PAGES);
            byte[] merged = PageStackComposer.stackVertically(pages, FALLBACK_MERGE_MAX_PIXELS);
            if (merged == null) {
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_500_001);
            }
            return new FileBytesResult(merged, "image/png", "png", file.fileNm());
        } catch (ApiException ae) {
            throw remapFileError(ae, DailyContractErrorCode.DAILYCONTRACT_500_001);
        } catch (Exception e) {
            log.error("계약서 PDF 폴백 병합 실패 — 원인={}", e.getMessage());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_500_001);
        }
    }

    /**
     * 사업장 인가 가드 — dailyentry 승인/거부 가드와 동일 기준 재사용(단일 출처).
     * 실패 시 계약서 도메인 코드(DAILYCONTRACT_403_001)로 매핑한다.
     */
    private void assertSiteAuthority(String cmpnyCd, String userCd, String authCd, String siteCd) {
        if (!dailyEntryService.hasSiteAuthority(cmpnyCd, userCd, authCd, siteCd)) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_403_001);
        }
    }

    /**
     * 계약서 양식 업로드 검증 (멀티페이지 지원 T2) — PDF/이미지 동시 지원.
     *
     * <p>검증 순서: 존재 → 크기 상한(10MB, 400_004) → contentType 화이트리스트(png/jpeg/pdf) →
     * <b>확장자-내용 정합</b>(선두 매직 바이트로 실제 포맷 판정) → 실검증(이미지=ImageIO 디코딩 /
     * PDF=암호 여부 + 페이지 수 1~20).
     *
     * <p>서명 PNG 업로드 검증({@link #validateImageUpload})과 분리해 두었다 — 서명 캔버스는 항상 PNG 이며
     * PDF 허용이 새어 들어가면 안 된다.
     *
     * <p>실제 포맷은 <b>선두 바이트</b>로만 판정한다(오프셋 0 의 "%PDF-"). 저장 확장자는 originalFilename 이
     * 결정하므로(FileService.resolveAllowedExtension), 확장자와 실제 내용이 어긋나면 열람 단계에서 깨진다.
     */
    private void validateContractUpload(MultipartFile file) {
        DailyContractErrorCode errorCode = DailyContractErrorCode.DAILYCONTRACT_400_004;
        if (file == null || file.isEmpty()) {
            throw new ApiException(errorCode);
        }
        if (file.getSize() > CONTRACT_FILE_MAX_BYTES) {
            log.info("일용직 계약서 업로드 크기 초과 — size={}, 상한={}", file.getSize(), CONTRACT_FILE_MAX_BYTES);
            throw new ApiException(errorCode);
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean declaredPdf = "application/pdf".equals(contentType);
        boolean declaredImage = "image/png".equals(contentType) || "image/jpeg".equals(contentType);
        if (!declaredPdf && !declaredImage) {
            log.info("일용직 계약서 업로드 형식 차단 — contentType={}", contentType);
            throw new ApiException(errorCode);
        }

        String ext = resolveUploadExt(file.getOriginalFilename());
        byte[] bytes = readBytesOrThrow(file, errorCode);

        if (PdfSupport.looksLikePdf(bytes)) {
            // 실제 내용 = PDF → 선언 contentType·확장자 모두 PDF 여야 한다(스푸핑/불일치 차단).
            if (!declaredPdf || !EXT_PDF.equals(ext)) {
                log.info("일용직 계약서 업로드 정합 실패(내용=PDF) — contentType={}, ext={}", contentType, ext);
                throw new ApiException(errorCode);
            }
            validatePdfContract(bytes, errorCode);
            return;
        }

        // 실제 내용 = 비 PDF → 이미지 선언·이미지 확장자만 허용 + ImageIO 실디코딩(polyglot 차단).
        if (!declaredImage || !("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext))) {
            log.info("일용직 계약서 업로드 정합 실패(내용=비PDF) — contentType={}, ext={}", contentType, ext);
            throw new ApiException(errorCode);
        }
        // 픽셀 수는 반드시 실디코딩 '전에' 헤더로 판정한다(sec SEC-1) — decodeImageOrThrow 가 먼저 돌면
        //   상한 초과 파일은 판정되기 전에 이미 힙을 점유한다. 판정 불가(UNKNOWN)는 여기서 막지 않고
        //   형식 유효성 판단을 실디코딩에 넘긴다.
        long imagePixels = ImageProbe.pixelCount(bytes);
        if (imagePixels != ImageProbe.UNKNOWN && imagePixels > CONTRACT_MAX_IMAGE_PIXELS) {
            log.info("일용직 계약서 업로드 차단(이미지 픽셀 수 초과) — pixels={}, 상한={}",
                    imagePixels, CONTRACT_MAX_IMAGE_PIXELS);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_008);
        }
        decodeImageOrThrow(file, errorCode);
    }

    /**
     * PDF 양식 실검증 — 암호 설정(400_006) 차단 + 페이지 수 1~20(400_005) + 페이지 치수(400_008) 검증.
     *
     * <p>치수 검증(sec SEC-1)이 없으면 페이지 수·바이트 상한을 통과한 대형 페이지 PDF 가 열람 시점에
     * OOM 을 유발한다. 발생 지점이 서명 게이트라 해당 사업장 전원 로그인이 막힌다.
     */
    private void validatePdfContract(byte[] bytes, DailyContractErrorCode errorCode) {
        try {
            if (PdfSupport.isEncrypted(bytes)) {
                log.info("일용직 계약서 업로드 차단(암호 설정 PDF)");
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_006);
            }
            int pageCount = PdfSupport.readPageCount(bytes);
            if (pageCount < 1) {
                log.info("일용직 계약서 업로드 차단(PDF 페이지 0장)");
                throw new ApiException(errorCode);
            }
            if (pageCount > CONTRACT_MAX_PAGES) {
                log.info("일용직 계약서 업로드 차단(페이지 수 초과) — pageCount={}, 상한={}",
                        pageCount, CONTRACT_MAX_PAGES);
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_005);
            }
            // 페이지 치수 게이트(sec SEC-1) — 열람 DPI 기준 최대 페이지의 렌더 픽셀 수로 판정한다.
            //   폴백 병합(100DPI)은 이보다 작으므로 150DPI 기준을 통과하면 함께 보호된다.
            long maxPagePixels = PdfSupport.maxPageRenderPixels(bytes, PAGE_RENDER_DPI);
            if (maxPagePixels > CONTRACT_MAX_PAGE_RENDER_PIXELS) {
                log.info("일용직 계약서 업로드 차단(페이지 렌더 픽셀 초과) — pixels={}, 상한={}, dpi={}",
                        maxPagePixels, CONTRACT_MAX_PAGE_RENDER_PIXELS, PAGE_RENDER_DPI);
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_008);
            }
        } catch (ApiException ae) {
            // FileService/PdfSupport 의 FILE_* 를 계약서 맥락 코드로 remap(손상 PDF → 400_004).
            throw remapFileError(ae, errorCode);
        }
    }

    /** 원본 파일명에서 확장자를 추출한다(점 제외 소문자, 경로 구분자 제거). 없으면 빈 문자열. */
    private String resolveUploadExt(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String safeName = originalFilename.replace("\\", "/");
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
        int dotIdx = safeName.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == safeName.length() - 1) {
            return "";
        }
        return safeName.substring(dotIdx + 1).toLowerCase();
    }

    /** multipart 바이트 읽기 — 실패 시 지정 에러코드. */
    private byte[] readBytesOrThrow(MultipartFile file, DailyContractErrorCode errorCode) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            log.info("일용직 계약서 업로드 바이트 읽기 실패 — 원인={}", e.getMessage());
            throw new ApiException(errorCode);
        }
    }

    /**
     * 공통 파일 계층(FileService/PdfSupport)의 FILE_* 예외를 계약서 도메인 코드로 remap 한다(plan §7).
     *
     * <p>암호 PDF(FILE_400_002) → 400_006, PDF 파싱/렌더 실패(FILE_500_001) → 호출 맥락 코드
     * (업로드=400_004 / 열람=500_001). 그 외(FILE_400_001 등)는 그대로 전파한다.
     */
    private ApiException remapFileError(ApiException ae, DailyContractErrorCode failCode) {
        if (FileErrorCode.FILE_400_002.equals(ae.getErrorCode())) {
            return new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_006);
        }
        if (FileErrorCode.FILE_500_001.equals(ae.getErrorCode())) {
            return new ApiException(failCode);
        }
        return ae;
    }

    /**
     * 이미지 업로드 공통 검증 — 존재/크기 상한/contentType(png·jpg) 화이트리스트.
     * 실제 디코딩 검증(polyglot 차단)은 {@link #decodeImageOrThrow} 또는 합성 시점에서 수행된다.
     *
     * <p>서명 PNG 전용 경로다(계약서 양식은 {@link #validateContractUpload}).
     */
    private void validateImageUpload(MultipartFile file, long maxBytes, DailyContractErrorCode errorCode) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(errorCode);
        }
        if (file.getSize() > maxBytes) {
            log.info("일용직 계약서 업로드 크기 초과 — size={}, 상한={}", file.getSize(), maxBytes);
            throw new ApiException(errorCode);
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            log.info("일용직 계약서 업로드 형식 차단 — contentType={}", contentType);
            throw new ApiException(errorCode);
        }
    }

    /** ImageIO 디코딩 검증 — 디코딩 실패(비이미지/손상/polyglot)는 즉시 차단. */
    private BufferedImage decodeImageOrThrow(MultipartFile file, DailyContractErrorCode errorCode) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ApiException(errorCode);
            }
            return image;
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.info("일용직 계약서/서명 이미지 디코딩 실패 — 원인={}", e.getMessage());
            throw new ApiException(errorCode);
        }
    }

    /** 계약서 도메인 파일 저장 — fileMgmtCd 채번 + FileService 위임(tbm01 saveSignatureFile 미러). */
    private String saveContractFile(String cmpnyCd, String userCd, String siteCd, MultipartFile file) {
        String fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(cmpnyCd, FILE_TYPE_DAILY_CONTRACT));
        fileService.fileSave(FileInfoParam.from(
                cmpnyCd, userCd, siteCd, FILE_TYPE_DAILY_CONTRACT, fileMgmtCd, file));
        return fileMgmtCd;
    }

    /** SHA-256 hex 계산(합성본 무결성 증적 — §6-3). */
    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            // SHA-256 은 JDK 필수 알고리즘 — 도달 불가 경로지만 방어적으로 래핑.
            throw new IllegalStateException("SHA-256 해시 계산 실패", e);
        }
    }
}
