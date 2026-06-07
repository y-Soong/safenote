package com.prafta.app.tbm.tbm01.service.impl;

import java.time.format.DateTimeFormatter;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.tbm.tbm01.application.command.TbmEnterCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmExitCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmPwdFailCommand;
import com.prafta.app.tbm.tbm01.application.param.TbmEnterParam;
import com.prafta.app.tbm.tbm01.application.param.TbmEntryContextParam;
import com.prafta.app.tbm.tbm01.application.param.TbmExitParam;
import com.prafta.app.tbm.tbm01.application.query.TbmPwdFailCountQuery;
import com.prafta.app.tbm.tbm01.application.query.TbmSessionQuery;
import com.prafta.app.tbm.tbm01.dto.response.TbmEnterResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEntryContextResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmExitResponse;
import com.prafta.app.tbm.tbm01.mapper.AppTbm01Mapper;
import com.prafta.app.tbm.tbm01.result.TbmAttendanceResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionResult;
import com.prafta.app.tbm.tbm01.service.AppTbm01Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-004-C: 앱 TBM 입실/종료(tbm01) 서비스 구현 (정규직 REGULAR MVP).
 *
 * <p>확정 결정 반영:
 *   <ul>
 *     <li>D1: 종료 시에만 서명 필수(입실 서명 없음).</li>
 *     <li>D2: USER_TYPE_CD='REGULAR' 고정(일용직 미구현).</li>
 *     <li>D3: STATUS_CD='OPENED' 일 때만 입실 허용.</li>
 *     <li>D4: 비밀번호 연속 5회 실패 시 1분 잠금(tb_tbm_pwd_fail 최근 카운트).</li>
 *     <li>D5: GPS AUTO=거리검증(반경 밖 차단), MANUAL/DISABLED=거리만 기록.
 *         응답/로그에 좌표 원본 노출 금지(거리 m만).</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppTbm01ServiceImpl implements AppTbm01Service {

    /** TB_FILE_INFO.FILE_TYPE — 003: TBM 서명(디렉토리 그룹). */
    private static final String FILE_TYPE_TBM_SIGN = "003";

    /** D4 잠금 정책: 최근 60초 이내 5회 실패 시 잠금. */
    private static final int PWD_LOCK_WINDOW_SECONDS = 60;
    private static final int PWD_LOCK_THRESHOLD = 5;

    private static final String STATUS_OPENED = "OPENED";
    /** prafta-051-08 C6: 종료 허용 상태 — 교육시작/교육종료 둘 다 허용. */
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String PWD_TYPE_ENTRY = "ENTRY";
    private static final String PWD_TYPE_EXIT = "EXIT";

    /** c-003: 종료 서명 파일 검증 - 허용 contentType 화이트리스트(PNG/JPEG). */
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    /** c-003: 종료 서명 파일 크기 상한(5MB). */
    private static final long SIGN_FILE_MAX_BYTES = 5L * 1024 * 1024;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppTbm01Mapper appTbm01Mapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    // -------------------------------------------------------------------------
    // C3: 입실 컨텍스트
    // -------------------------------------------------------------------------
    @Override
    public TbmEntryContextResponse selectEntryContext(TbmEntryContextParam param) {

        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();
        String userCd = token.gv_userCd();

        TbmSessionResult session = appTbm01Mapper.selectSession(
                TbmSessionQuery.from(cmpnyCd, siteCd, param.sessionCd(), userCd));
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        TbmAttendanceResult attendance = appTbm01Mapper.selectMyAttendance(
                TbmSessionQuery.from(cmpnyCd, siteCd, param.sessionCd(), userCd));
        boolean alreadyEntered = attendance != null && attendance.getEntryAt() != null;

        return TbmEntryContextResponse.builder()
                .sessionCd(session.getSessionCd())
                .title(session.getTitle())
                .statusCd(session.getStatusCd())
                .gpsVerifyTypeCd(session.getGpsVerifyTypeCd())
                .gpsVerifyRadiusM(session.getGpsVerifyRadiusM())
                .entryAvailable(STATUS_OPENED.equals(session.getStatusCd()))
                .alreadyEntered(alreadyEntered)
                .requiresExitSignature(true) // D1
                .build();
    }

    // -------------------------------------------------------------------------
    // C1: 입실
    // -------------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbmEnterResponse enter(TbmEnterParam param) {

        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();
        String userCd = token.gv_userCd();
        String sessionCd = param.sessionCd();

        TbmSessionResult session = appTbm01Mapper.selectSession(
                TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        // D3: OPENED 일 때만 입실 허용.
        if (!STATUS_OPENED.equals(session.getStatusCd())) {
            log.info("[tbm01] 입실 불가 상태: sessionCd={}, status={}", sessionCd, session.getStatusCd());
            throw new ApiException(TbmErrorCode.TBM_409_030);
        }

        // 기입실(멱등): UNIQUE 충돌 전에 선조회로 빠르게 안내.
        TbmAttendanceResult existing = appTbm01Mapper.selectMyAttendance(
                TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
        if (existing != null && existing.getEntryAt() != null) {
            return idempotentEnterResponse(existing);
        }

        // D4: 비밀번호 잠금 검사 → 검증 → 실패 시 기록.
        verifyPassword(cmpnyCd, sessionCd, userCd, PWD_TYPE_ENTRY,
                session.getEntryPwd(), param.entryPwd());

        // D5: GPS 거리 계산/검증.
        Integer distanceM = resolveDistanceAndVerify(session, param.lat(), param.lon());

        // 출결 INSERT (UNIQUE 충돌 = 동시성 멱등).
        TbmEnterCommand command = TbmEnterCommand.of(
                cmpnyCd, sessionCd, userCd, param.lat(), param.lon(), distanceM);
        try {
            appTbm01Mapper.insertAttendance(command);
        } catch (DuplicateKeyException dke) {
            // 동시 요청으로 이미 입실 처리됨 → 기존 출결로 멱등 응답.
            TbmAttendanceResult after = appTbm01Mapper.selectMyAttendance(
                    TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
            if (after != null && after.getEntryAt() != null) {
                return idempotentEnterResponse(after);
            }
            log.error("[tbm01] 입실 UNIQUE 충돌 후 출결 재조회 실패: sessionCd={}", sessionCd, dke);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }

        // 채번된 출결 재조회(응답 ATTENDANCE_CD/ENTRY_AT 확정).
        TbmAttendanceResult saved = appTbm01Mapper.selectMyAttendance(
                TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
        if (saved == null) {
            log.error("[tbm01] 입실 직후 출결 조회 실패: sessionCd={}", sessionCd);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }

        log.info("[tbm01] 입실 완료: attendanceCd={}, distanceM={}", saved.getAttendanceCd(), distanceM);
        return TbmEnterResponse.builder()
                .attendanceCd(saved.getAttendanceCd())
                .entryAt(formatDate(saved.getEntryAt()))
                .entryDistanceM(saved.getEntryDistanceM())
                .alreadyEntered(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // C2: 종료
    // -------------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbmExitResponse exit(TbmExitParam param) {

        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();
        String userCd = token.gv_userCd();
        String sessionCd = param.sessionCd();

        TbmSessionResult session = appTbm01Mapper.selectSession(
                TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        // prafta-051-08 C6: 종료는 교육시작(IN_PROGRESS)/교육종료(COMPLETED) 상태에서만 허용.
        // (개설준비 OPENED 단계는 아직 교육 진행 전이므로 종료 불가)
        String statusCd = session.getStatusCd();
        if (!STATUS_IN_PROGRESS.equals(statusCd) && !STATUS_COMPLETED.equals(statusCd)) {
            log.info("[tbm01] 종료 불가 상태: sessionCd={}, status={}", sessionCd, statusCd);
            throw new ApiException(TbmErrorCode.TBM_409_033);
        }

        // 본인 입실 기록 확인.
        TbmAttendanceResult attendance = appTbm01Mapper.selectMyAttendance(
                TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
        if (attendance == null || attendance.getEntryAt() == null) {
            throw new ApiException(TbmErrorCode.TBM_409_031);
        }
        if (attendance.getExitAt() != null) {
            throw new ApiException(TbmErrorCode.TBM_409_032);
        }

        // D1: 종료 서명 필수.
        MultipartFile signFile = param.signFile();
        if (signFile == null || signFile.isEmpty()) {
            throw new ApiException(TbmErrorCode.TBM_400_031);
        }

        // c-003: 서명 파일 타입/크기 서버측 검증(임의 확장자/대용량 업로드 차단).
        validateSignatureFile(signFile);

        // D4: 비밀번호 잠금 검사 → 검증 → 실패 시 기록.
        verifyPassword(cmpnyCd, sessionCd, userCd, PWD_TYPE_EXIT,
                session.getExitPwd(), param.exitPwd());

        // 종료 서명 파일 저장 → EXIT_SIGN_FILE_MGMT_CD.
        String signFileMgmtCd = saveSignatureFile(cmpnyCd, userCd, session.getSiteCd(), signFile);

        // 출결 UPDATE(본인+미종료만).
        // prafta-051-08: appForegroundSec(앱 포그라운드 누적초, nullable) 동반 저장.
        //   SELF_DEVICE 본인 종료 경로이므로 자연히 본인 출결에만 값이 기록된다(대리/검색입실 NULL).
        TbmExitCommand command = TbmExitCommand.of(
                cmpnyCd, sessionCd, userCd, signFileMgmtCd, param.appForegroundSec());
        int updated = appTbm01Mapper.updateExit(command);
        if (updated == 0) {
            // 동시성: 그 사이 종료됨.
            throw new ApiException(TbmErrorCode.TBM_409_032);
        }

        log.info("[tbm01] 종료 완료: attendanceCd={}", attendance.getAttendanceCd());
        return TbmExitResponse.builder()
                .attendanceCd(attendance.getAttendanceCd())
                .exitAt(java.time.LocalDateTime.now().format(DT))
                .completionStatusCd(command.completionStatusCd())
                .build();
    }

    // -------------------------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------------------------

    /** D4: 잠금 검사 → 비밀번호 검증 → 실패 시 로그, 성공 시 카운터 정리. */
    private void verifyPassword(String cmpnyCd, String sessionCd, String userCd,
                                String pwdTypeCd, String storedPwd, String inputPwd) {

        // 잠금 검사(최근 윈도우 내 임계 도달 시 거부).
        int recentFail = appTbm01Mapper.countRecentPwdFail(
                TbmPwdFailCountQuery.from(cmpnyCd, sessionCd, pwdTypeCd, userCd, PWD_LOCK_WINDOW_SECONDS));
        if (recentFail >= PWD_LOCK_THRESHOLD) {
            log.info("[tbm01] 비밀번호 잠금: sessionCd={}, type={}, recentFail={}", sessionCd, pwdTypeCd, recentFail);
            throw new ApiException(TbmErrorCode.TBM_429_030);
        }

        boolean match = StringUtils.hasText(storedPwd) && storedPwd.equals(inputPwd);
        if (!match) {
            appTbm01Mapper.insertPwdFail(
                    TbmPwdFailCommand.of(cmpnyCd, sessionCd, pwdTypeCd, userCd));
            log.info("[tbm01] 비밀번호 불일치: sessionCd={}, type={}", sessionCd, pwdTypeCd);
            throw new ApiException(TbmErrorCode.TBM_400_030);
        }

        // 성공 → 해당 세션/유형/시도자 실패 카운터 정리.
        appTbm01Mapper.deletePwdFail(TbmPwdFailCommand.of(cmpnyCd, sessionCd, pwdTypeCd, userCd));
    }

    /**
     * D5: 거리 계산 및 검증.
     * <p>AUTO: 좌표/세션좌표로 거리 산출, 반경 초과 시 차단. MANUAL/DISABLED: 거리만 기록(차단 안 함).
     * @return 거리(m). 계산 불가(좌표/세션좌표 부재)면 null.
     */
    private Integer resolveDistanceAndVerify(TbmSessionResult session, Double lat, Double lon) {

        String verifyType = session.getGpsVerifyTypeCd();

        // DISABLED: 검증 생략(거리 미산출).
        if ("DISABLED".equals(verifyType)) {
            return null;
        }

        Integer distanceM = null;
        if (lat != null && lon != null
                && session.getManagerGpsLat() != null && session.getManagerGpsLon() != null) {
            double d = haversineMeters(
                    lat, lon,
                    session.getManagerGpsLat().doubleValue(),
                    session.getManagerGpsLon().doubleValue());
            distanceM = (int) Math.round(d);
        }

        if ("AUTO".equals(verifyType)) {
            // AUTO 인데 좌표 또는 세션좌표가 없으면 거리 검증 불가 → 차단(부정확 입실 방지).
            if (distanceM == null) {
                log.info("[tbm01] AUTO GPS 검증 불가(좌표 부재): sessionCd={}", session.getSessionCd());
                throw new ApiException(TbmErrorCode.TBM_403_030);
            }
            int radius = session.getGpsVerifyRadiusM() != null ? session.getGpsVerifyRadiusM() : 100;
            if (distanceM > radius) {
                log.info("[tbm01] GPS 반경 초과: sessionCd={}, distanceM={}, radius={}",
                        session.getSessionCd(), distanceM, radius);
                throw new ApiException(TbmErrorCode.TBM_403_030);
            }
        }
        // MANUAL: 거리만 기록(관리자 확인 기반, 차단 안 함).
        return distanceM;
    }

    /**
     * c-003: 종료 서명 파일 서버측 검증.
     * <p>contentType 화이트리스트(PNG/JPEG) + 크기 상한(5MB) + 매직바이트 확인.
     * <p>위반 시 TBM_400_031(서명 등록 안내)로 거부한다(신규 코드 미추가).
     */
    private void validateSignatureFile(MultipartFile file) {

        // 1) 크기 상한.
        if (file.getSize() > SIGN_FILE_MAX_BYTES) {
            log.info("[tbm01] 서명 파일 크기 초과: size={}", file.getSize());
            throw new ApiException(TbmErrorCode.TBM_400_031);
        }

        // 2) contentType 화이트리스트(PNG/JPEG 만 허용).
        String contentType = file.getContentType();
        boolean allowedType = CONTENT_TYPE_PNG.equals(contentType) || CONTENT_TYPE_JPEG.equals(contentType);
        if (!allowedType) {
            log.info("[tbm01] 서명 파일 타입 불허: contentType={}", contentType);
            throw new ApiException(TbmErrorCode.TBM_400_031);
        }

        // 3) 매직바이트 확인(확장자 위장 차단). PNG: 89 50 4E 47, JPEG: FF D8.
        try {
            byte[] head = new byte[4];
            int read;
            try (java.io.InputStream in = file.getInputStream()) {
                read = in.read(head);
            }
            boolean isPng = read >= 4
                    && (head[0] & 0xFF) == 0x89 && (head[1] & 0xFF) == 0x50
                    && (head[2] & 0xFF) == 0x4E && (head[3] & 0xFF) == 0x47;
            boolean isJpeg = read >= 2
                    && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8;
            if (!isPng && !isJpeg) {
                log.info("[tbm01] 서명 파일 매직바이트 불일치: contentType={}", contentType);
                throw new ApiException(TbmErrorCode.TBM_400_031);
            }
        } catch (java.io.IOException e) {
            log.error("[tbm01] 서명 파일 검증 중 읽기 오류", e);
            throw new ApiException(TbmErrorCode.TBM_400_031);
        }
    }

    /** 종료 서명 파일 저장 → fileMgmtCd 발급. risk01 파일 저장 패턴 차용. */
    private String saveSignatureFile(String cmpnyCd, String userCd, String siteCd, MultipartFile file) {

        String fileMgmtCd = fileMapper.selectFileMgmtCd(
                FileInfoQuery.from(cmpnyCd, FILE_TYPE_TBM_SIGN));

        // siteCd 가 token 에 없으면 파일 경로 안전성 위해 회사코드 폴백.
        String safeSiteCd = StringUtils.hasText(siteCd) ? siteCd : cmpnyCd;

        fileService.fileSave(FileInfoParam.from(
                cmpnyCd, userCd, safeSiteCd, FILE_TYPE_TBM_SIGN, fileMgmtCd, file));

        return fileMgmtCd;
    }

    private TbmEnterResponse idempotentEnterResponse(TbmAttendanceResult existing) {
        return TbmEnterResponse.builder()
                .attendanceCd(existing.getAttendanceCd())
                .entryAt(formatDate(existing.getEntryAt()))
                .entryDistanceM(existing.getEntryDistanceM())
                .alreadyEntered(true)
                .build();
    }

    private String formatDate(java.util.Date date) {
        if (date == null) return null;
        return java.time.LocalDateTime
                .ofInstant(date.toInstant(), java.time.ZoneId.systemDefault())
                .format(DT);
    }

    /** WGS84 평균반경 기준 두 좌표 간 대권거리(m). (app-002 AppAttd01ServiceImpl 공식 동형) */
    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusM = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }
}
