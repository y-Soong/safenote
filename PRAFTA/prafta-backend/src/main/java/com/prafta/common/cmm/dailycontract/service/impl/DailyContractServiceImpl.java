package com.prafta.common.cmm.dailycontract.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.dailycontract.ContractImageComposer;
import com.prafta.common.cmm.dailycontract.application.command.ContractInsertCommand;
import com.prafta.common.cmm.dailycontract.application.command.ContractSignInsertCommand;
import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.mapper.DailyContractMapper;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.result.DailyUserMetaResult;
import com.prafta.common.cmm.dailycontract.result.SignGateResult;
import com.prafta.common.cmm.dailycontract.service.DailyContractService;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일용직 근로계약서 core 서비스 구현.
 *
 * <p>파일 저장/로드는 공통 {@code FileService}(file.upload.base-dir 외부화 경로 + tb_file_info 적재 +
 * 확장자 화이트리스트/traversal 방어)를 재사용한다. 파일 경로는 응답에 노출하지 않는다(스트림만).
 *
 * <p>게이트 판정(D8, plan §6 기본안 3): 활성 계약서 없음 → 스킵 / 현재 버전 서명 없음 → 필요 /
 * 서명의 REQ_ID 가 최근 소진('05') 요청과 불일치 → 필요(재입장 재서명) / REQ_ID NULL 레거시는 버전만 비교.
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

        // 2) 활성 계약서 미등록 사업장 → 스킵(R2 — 필수약관 "미등록=스킵" 패턴 미러).
        ActiveContractResult active = dailyContractMapper.selectActiveContract(cmpnyCd, meta.siteCd());
        if (active == null) {
            return SignGateResult.skip();
        }

        // 3) 현재 버전 서명 존재 + 사이클 일치 여부(D8-①·②).
        if (isCurrentCycleSigned(cmpnyCd, userCd, active.contractVer())) {
            return SignGateResult.skip();
        }
        return SignGateResult.required(active.contractVer(), active.contractNm());
    }

    /**
     * 현재 버전·현재 승인 사이클 서명 완료 여부 (게이트 판정/서명 멱등 가드 공용).
     *
     * <p>판정: 현재 버전 서명 존재 AND (서명 REQ_ID == 최근 소진('05') 요청 ID
     * OR 서명 REQ_ID IS NULL(레거시 — 버전만 비교) OR 소진 요청 자체가 없음(배포 전 활성 계정)).
     */
    private boolean isCurrentCycleSigned(String cmpnyCd, String userCd, int contractVer) {
        ContractSignMetaResult latestSign = dailyContractMapper.selectLatestSignForVer(cmpnyCd, userCd, contractVer);
        if (latestSign == null) {
            return false;
        }
        if (latestSign.reqId() == null || latestSign.reqId().isBlank()) {
            // 레거시 서명(승인 사이클 미기록) — 버전 일치만으로 서명 완료 취급.
            return true;
        }
        String latestConsumedReqId = dailyContractMapper.selectLatestConsumedReqId(cmpnyCd, userCd);
        if (latestConsumedReqId == null || latestConsumedReqId.isBlank()) {
            // 소진 요청 없음(배포 전 기존 활성 계정 등) — 버전 일치만으로 판정.
            return true;
        }
        // 서명 사이클 == 최근 소진 사이클이면 완료, 다르면 재입장 재서명 필요(D8-①).
        return latestConsumedReqId.equals(latestSign.reqId());
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

        // 업로드 검증 — 이미지(png/jpg) + 10MB 이하 + 실제 디코딩 성공(polyglot 차단).
        validateImageUpload(file, CONTRACT_FILE_MAX_BYTES, DailyContractErrorCode.DAILYCONTRACT_400_004);
        // 실디코딩 검증(SEC-2) — contentType 은 클라이언트 신고값이라 스푸핑 가능. 디코딩 불가 파일이
        // 활성 계약서로 등록되면 해당 사업장 전체 일용직의 서명 합성이 전면 실패하므로 업로드 시점에 차단.
        decodeImageOrThrow(file, DailyContractErrorCode.DAILYCONTRACT_400_004);

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
    public List<ContractVersionRow> findContractVersions(String cmpnyCd, String siteCd,
            String procUserCd, String procAuthCd) {
        if (siteCd == null || siteCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, siteCd);

        return dailyContractMapper.selectContractVersionList(cmpnyCd, siteCd, LIST_LIMIT);
    }

    @Override
    public ImageBytesResult loadContractImageForAdmin(String cmpnyCd, String siteCd, int contractVer,
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

        ImageBytesResult image = fileService.loadImageBytes(new FileReadQuery(cmpnyCd, fileMgmtCd));
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
    public ImageBytesResult loadActiveContractImageForUser(String cmpnyCd, String userCd) {
        DailyUserMetaResult meta = dailyContractMapper.selectDailyUserMeta(cmpnyCd, userCd);
        if (meta == null) {
            return null;
        }
        ActiveContractResult active = dailyContractMapper.selectActiveContract(cmpnyCd, meta.siteCd());
        if (active == null) {
            return null;
        }

        ImageBytesResult image = fileService.loadImageBytes(new FileReadQuery(cmpnyCd, active.fileMgmtCd()));
        if (image == null) {
            // 활성 계약서 행은 있는데 원본 파일이 없음 — 일용직에게 "미등록"으로 안내되면 안 된다(관리자 재등록 필요).
            log.error("일용직 계약서 원본 파일 없음 — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), active.contractVer());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }
        return image;
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

        // 3) 활성 계약서 필수.
        ActiveContractResult active = dailyContractMapper.selectActiveContract(cmpnyCd, meta.siteCd());
        if (active == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        // 4) 멱등 가드 — 현재 버전·현재 사이클 서명 존재 시 400_002.
        if (isCurrentCycleSigned(cmpnyCd, userCd, active.contractVer())) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_002);
        }

        // 5) 계약서 원본 로드(경로 방어/화이트리스트는 FileService 가 수행).
        ImageBytesResult contractImage = fileService.loadImageBytes(new FileReadQuery(cmpnyCd, active.fileMgmtCd()));
        if (contractImage == null) {
            log.error("일용직 계약서 원본 파일 없음(서명 불가) — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), active.contractVer());
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_002);
        }

        // 6) 합성(§4-3 자동 계약정보 블록) — 서명일시/최초 근로일은 서버 시각만(§6-3, D1).
        LocalDateTime now = LocalDateTime.now();
        String firstWorkDate = now.toLocalDate().format(YMD);
        byte[] mergedBytes;
        try {
            mergedBytes = ContractImageComposer.compose(
                    contractImage.data()
                    , signImage
                    , meta.userNm()
                    , now.toLocalDate().format(DISP_DATE)
                    , now.format(DISP_DTIME));
        } catch (Exception e) {
            log.error("일용직 계약서 합성 실패 — cmpnyCd={}, siteCd={}, ver={}",
                    cmpnyCd, meta.siteCd(), active.contractVer(), e);
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_001);
        }

        // 7) SHA-256(합성본) — 증적 무결성(§6-3).
        String mergedSha256 = sha256Hex(mergedBytes);

        // 8) 파일 저장 — 서명 PNG 원본 + 합성본(둘 다 tb_file_info 적재, FILE_UPLOAD 외부화 경로).
        String signFileMgmtCd = saveContractFile(cmpnyCd, userCd, meta.siteCd(), signFile);
        String mergedFileMgmtCd = saveContractFile(cmpnyCd, userCd, meta.siteCd(),
                new BytesMultipartFile("file", "contract-merged.png", "image/png", mergedBytes));

        // 9) append-only INSERT — 현재 승인 사이클(최근 소진 REQ_ID) 연결. 레거시(소진 없음)는 NULL.
        String reqId = dailyContractMapper.selectLatestConsumedReqId(cmpnyCd, userCd);
        String signId = dailyContractMapper.selectSignId(cmpnyCd);
        dailyContractMapper.insertContractSign(new ContractSignInsertCommand(
                cmpnyCd
                , signId
                , meta.siteCd()
                , userCd
                , meta.userNm()
                , active.contractVer()
                , reqId
                , signFileMgmtCd
                , mergedFileMgmtCd
                , mergedSha256
                , firstWorkDate));

        log.info("일용직 계약서 서명 완료 — cmpnyCd={}, userCd={}, signId={}, ver={}, reqId={}",
                cmpnyCd, userCd, signId, active.contractVer(), reqId);

        return dailyContractMapper.selectSignMetaById(cmpnyCd, signId);
    }

    @Override
    public ContractSignMetaResult findMySign(String cmpnyCd, String userCd) {
        return dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
    }

    @Override
    public ImageBytesResult loadMySignImage(String cmpnyCd, String userCd) {
        // 본인 스코프 강제 — (cmpnyCd, userCd)로만 조회하므로 타인 서명본 접근 불가(IDOR 차단).
        ContractSignMetaResult mySign = dailyContractMapper.selectMySignLatest(cmpnyCd, userCd);
        if (mySign == null) {
            return null;
        }
        return fileService.loadImageBytes(new FileReadQuery(cmpnyCd, mySign.mergedFileMgmtCd()));
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
    public ImageBytesResult loadSignImageForAdmin(String cmpnyCd, String signId,
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

        return fileService.loadImageBytes(new FileReadQuery(cmpnyCd, sign.mergedFileMgmtCd()));
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
     * 이미지 업로드 공통 검증 — 존재/크기 상한/contentType(png·jpg) 화이트리스트.
     * 실제 디코딩 검증(polyglot 차단)은 {@link #decodeImageOrThrow} 또는 합성 시점에서 수행된다.
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
