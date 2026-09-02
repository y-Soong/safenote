package com.prafta.app.tbm.tbm01.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.tbm.tbm01.application.command.TbmEnterCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmExitCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmLeaveBeforeCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmWithdrawCommand;
import com.prafta.app.tbm.tbm01.application.param.TbmEnterParam;
import com.prafta.app.tbm.tbm01.application.param.TbmEntryContextParam;
import com.prafta.app.tbm.tbm01.application.param.TbmExitParam;
import com.prafta.app.tbm.tbm01.application.param.TbmSessionDetailParam;
import com.prafta.app.tbm.tbm01.application.param.TbmSessionListParam;
import com.prafta.app.tbm.tbm01.application.query.TbmDetailQuery;
import com.prafta.app.tbm.tbm01.application.query.TbmSessionListQuery;
import com.prafta.app.tbm.tbm01.application.query.TbmSessionQuery;
import com.prafta.app.tbm.tbm01.dto.response.TbmActionResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmAttendeeListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmCompletionResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmContentResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEnterResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEntryContextResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmExitResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmMyAttendanceResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmRiskListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionStateResponse;
import com.prafta.app.tbm.tbm01.mapper.AppTbm01Mapper;
import com.prafta.app.tbm.tbm01.result.TbmAttendanceResult;
import com.prafta.app.tbm.tbm01.result.TbmAttendanceSlotResult;
import com.prafta.app.tbm.tbm01.result.TbmAttendeeResult;
import com.prafta.app.tbm.tbm01.result.TbmCompletionResult;
import com.prafta.app.tbm.tbm01.result.TbmContentItemResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionContentResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionListResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionRiskResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionStateResult;
import com.prafta.app.tbm.tbm01.service.AppTbm01Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.tbmshare.result.TbmSessionAccess;
import com.prafta.common.cmm.tbmshare.service.TbmSessionShareService;
import com.prafta.common.cmm.worktime.service.WorktimeGateService;
import com.prafta.common.cmm.location.service.LocationConsentService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.error.location.LocationErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.FileUrlSigner;
import com.prafta.common.security.crypto.GpsCoordCrypto;

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
 *     <li>비밀번호: 현장 공유 passcode 특성상 시도 횟수 제한(잠금) 없음. 불일치만 거부.</li>
 *     <li>D5: GPS AUTO=거리검증(반경 밖 차단), MANUAL/DISABLED=거리만 기록.
 *         응답/로그에 좌표 원본 노출 금지(거리 m만).</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppTbm01ServiceImpl implements AppTbm01Service {

    /**
     * TB_FILE_INFO.FILE_TYPE — 009: TBM 서명(디렉토리 그룹, 보호 파일타입).
     * <p>security H-1 후속(2026-08-31): 자필 서명은 PII 이므로 교육자료('003', 공개 정적 서빙)에서
     * 분리해 서명 전용 타입으로 채번했다. FileServiceImpl.PROTECTED_FILE_TYPES 에 포함되어
     * secure base 에 저장되며(무인증 /uploads/** 정적 열람 불가), 인증 스트림 EP
     * (GET /appApi/tbm/my-sign-image)로만 서빙된다.
     */
    private static final String FILE_TYPE_TBM_SIGN = "009";

    /** 앱 사용자 TBM 은 정규직 고정(D2). grandfather 판정(M4)의 사용자 유형에도 동일 적용. */
    private static final String USER_TYPE_REGULAR = "REGULAR";

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
    private final FileUrlSigner fileUrlSigner;   // 파일 서빙 서명 URL 발급(공통 인프라)

    /** prafta-app-022: TBM 입실 근무중 게이트(근무중에만 입실 허용). */
    private final WorktimeGateService worktimeGateService;

    /** PRAFTA-SUBCON-T5: 연동 회사 지정 공통 검증 지점(세션 접근/입실 범위/개최사 라벨). */
    private final TbmSessionShareService tbmSessionShareService;

    /** GPS좌표-암호화-전환-06/-07: 좌표 AES-GCM 암복호화(입실 저장 암호화 + 세션 좌표 fallback 복호화). */
    private final GpsCoordCrypto gpsCoordCrypto;

    /** 위치정보 동의 판정 단일 출처(위치정보 동의철회·중지 S3) — 미동의 시 좌표를 저장하지 않는다. */
    private final LocationConsentService locationConsentService;

    // -------------------------------------------------------------------------
    // C3: 입실 컨텍스트
    // -------------------------------------------------------------------------
    @Override
    public TbmEntryContextResponse selectEntryContext(TbmEntryContextParam param) {

        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();
        String userCd = token.gv_userCd();

        // PRAFTA-SUBCON-T5: 세션 접근 게이트(자사 세션은 사업장 스코프 유지, 타사 세션은 지정 체인 판정).
        TbmSessionQuery query = viewableQuery(param.sessionCd(), cmpnyCd, siteCd, userCd);

        TbmSessionResult session = appTbm01Mapper.selectSession(query);
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        TbmAttendanceResult attendance = appTbm01Mapper.selectMyAttendance(query);
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

        // PRAFTA-SUBCON-T5(P1): 세션 접근 게이트 → 개설사 확보(미지정 회사에는 404 존재 비노출).
        TbmSessionQuery query = viewableQuery(sessionCd, cmpnyCd, siteCd, userCd);

        TbmSessionResult session = appTbm01Mapper.selectSession(query);
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        // prafta-app-022: 근무중 게이트 — 근무 중에만 TBM 입실 허용(정책 safety §2).
        //   세션 존재 확인(404) 직후, OPENED 검증/비번 검증/멱등 분기보다 앞에서 차단한다.
        //   exit/leaveBefore/withdraw 에는 적용하지 않는다(입실만 게이트).
        //   타사 세션이어도 본인은 자기 회사 근무 상태 기준으로 판정한다(무변경).
        worktimeGateService.assertWorking(token);

        // ★위치정보 동의 게이트(S4) — 동의(AGREED) 상태가 아니면 입실을 차단한다.
        //   ★GPS 검증을 쓰지 않는 세션(DISABLED/MANUAL)도 함께 차단한다 — 사용자 확정 설계상
        //     "미동의면 서비스 이용 불가"이고, 세션 유형에 따라 통과 여부가 갈리면 사용자가
        //     자기 상태를 이해할 수 없다(같은 화면에서 어떤 TBM 은 되고 어떤 건 안 되는 상태).
        //   전용 오류코드로 앱이 안내 팝업 → 재동의 화면으로 분기한다.
        if (!locationConsentService.isCollectAllowed(cmpnyCd, userCd)) {
            log.info("[tbm01] 입실 거부: 위치정보 미동의 (userCd={}, sessionCd={})", userCd, sessionCd);
            throw new ApiException(LocationErrorCode.LOCATION_403_001);
        }

        // PRAFTA-SUBCON-T5 공통 게이트(P1): 참석자 소속 회사 ∈ {개설사} ∪ SHARE 체인.
        //   출결 INSERT 를 수행하는 모든 경로가 통과해야 하는 단일 지점(요청서 §3.2).
        tbmSessionShareService.assertEntryAllowed(sessionCd, cmpnyCd);

        // D3: 세션이 살아있는 동안만(OPENED/IN_PROGRESS) 입실 API 진입 허용. 신규 입실이 OPENED
        //     에서만 되도록 막는 건 아래 D3-2(멱등 분기 이후)에서 별도 판정한다 — 여기서 IN_PROGRESS
        //     를 통째로 막으면 "교육중" 탭의 기입실자 재인증(멱등 재진입)까지 함께 막혀버린다
        //     (2026-08-23 실기기 검증에서 발견: 이미 입실한 근로자가 자동 교육시작 이후 본인 세션에
        //     재진입 자체를 못 하던 결함 — TBM_409_030). COMPLETED/CANCELLED/DRAFT 는 그대로 차단.
        boolean sessionActive = STATUS_OPENED.equals(session.getStatusCd())
                || STATUS_IN_PROGRESS.equals(session.getStatusCd());
        if (!sessionActive) {
            log.info("[tbm01] 입실 불가 상태: sessionCd={}, status={}", sessionCd, session.getStatusCd());
            throw new ApiException(TbmErrorCode.TBM_409_030);
        }

        // 비밀번호 검증. 반드시 기입실 멱등 분기보다 먼저 수행한다. 비번 검증을 멱등 분기 뒤에 두면
        // 1회 입실(종료 전) 이후에는 잘못된 비밀번호로도 멱등 응답으로 입실 통과되는
        // 검증 우회가 발생한다(맞는 비밀번호로만 입실 허용).
        verifyPassword(PWD_TYPE_ENTRY, session.getEntryPwd(), param.entryPwd());

        // 기입실(멱등): 비밀번호가 일치한 경우에만, UNIQUE 충돌 전에 선조회로 빠르게 안내.
        //   OPENED/IN_PROGRESS 어느 쪽이든 이미 입실한 사람은 통과(교육중 탭 재진입 지원).
        TbmAttendanceResult existing = appTbm01Mapper.selectMyAttendance(query);
        if (existing != null && existing.getEntryAt() != null) {
            return idempotentEnterResponse(existing);
        }

        // D3-2: 여기까지 왔다는 건 출결이 없는 "신규" 입실 시도 — OPENED 일 때만 허용한다.
        //   IN_PROGRESS 로 전환된 뒤의 지각 신규 입실은 정책상 차단(safety §2.2).
        if (!STATUS_OPENED.equals(session.getStatusCd())) {
            log.info("[tbm01] 신규 입실 불가(교육시작 이후 지각): sessionCd={}, status={}", sessionCd, session.getStatusCd());
            throw new ApiException(TbmErrorCode.TBM_409_030);
        }

        // D5: GPS 거리 계산/검증 — 암호화 전 원본 Double 좌표로 기존 위치에서 수행(판정 무변경).
        Integer distanceM = resolveDistanceAndVerify(session, param.lat(), param.lon());

        // 출결 슬롯(UNIQUE 키) 선조회 → INSERT(신규)/RESTORE(내보내기 후 재입실) 분기.
        // (managerEnter 의 동일 패턴 이식 — 관리자 "내보내기"가 소프트삭제(DEL_YN='Y')라
        //  UNIQUE 키가 여전히 점유돼 있어, 무조건 INSERT 하면 중복키 충돌 후 갈 곳이 없어 500 이 났었다.)
        // GPS좌표-암호화-전환-07: 좌표는 암호문만 저장(BigDecimal.valueOf 경유 정규화 — 좌표 결측이면 null).
        //   ★위치정보 동의철회·중지 S3: 동의(AGREED) 상태가 아니면 좌표를 저장하지 않는다.
        //     거리 검증(resolveDistanceAndVerify)은 원본 좌표로 이미 수행됐다 — 판정에는 쓰고 저장만 하지 않는다.
        //     ENTRY_DISTANCE_M 은 좌표가 아니므로 그대로 남긴다(3년 파기 배치의 대상 범위와 동일 기준).
        boolean gpsAllowed = locationConsentService.isCollectAllowed(cmpnyCd, userCd);
        TbmEnterCommand command = TbmEnterCommand.of(
                cmpnyCd, sessionCd, userCd,
                gpsAllowed ? gpsCoordCrypto.encrypt(param.lat()) : null,
                gpsAllowed ? gpsCoordCrypto.encrypt(param.lon()) : null, distanceM);

        TbmAttendanceSlotResult slot = appTbm01Mapper.selectAttendanceSlot(command);
        if (slot == null) {
            try {
                appTbm01Mapper.insertAttendance(command);
            } catch (DuplicateKeyException dke) {
                // 동시 요청으로 이미 입실 처리됨 → 기존 출결로 멱등 응답.
                TbmAttendanceResult after = appTbm01Mapper.selectMyAttendance(query);
                if (after != null && after.getEntryAt() != null) {
                    return idempotentEnterResponse(after);
                }
                log.error("[tbm01] 입실 UNIQUE 충돌 후 출결 재조회 실패: sessionCd={}", sessionCd, dke);
                throw new ApiException(CommonErrorCode.COMMON_500_001);
            }
        } else if ("N".equals(slot.delYn())) {
            // 이미 입실됨(위의 existing 선조회와 사실상 동시 진입 케이스만 남음) → 멱등 응답.
            TbmAttendanceResult after = appTbm01Mapper.selectMyAttendance(query);
            if (after != null && after.getEntryAt() != null) {
                return idempotentEnterResponse(after);
            }
            log.error("[tbm01] 입실 슬롯 DEL_YN=N 인데 출결 재조회 실패: sessionCd={}", sessionCd);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        } else {
            // 관리자 "내보내기"(soft delete) 후 본인 재입실 → RESTORE.
            int affected = appTbm01Mapper.restoreAttendance(command);
            if (affected == 0) {
                // 경합으로 슬롯 상태 변경됨(예: 그 사이 관리자가 대리입실시킴) → 재조회 후 멱등 응답, 없으면 500.
                TbmAttendanceResult after = appTbm01Mapper.selectMyAttendance(query);
                if (after != null && after.getEntryAt() != null) {
                    return idempotentEnterResponse(after);
                }
                log.error("[tbm01] 입실 RESTORE 경합 후 출결 재조회 실패: sessionCd={}", sessionCd);
                throw new ApiException(CommonErrorCode.COMMON_500_001);
            }
        }

        // 채번된 출결 재조회(응답 ATTENDANCE_CD/ENTRY_AT 확정).
        TbmAttendanceResult saved = appTbm01Mapper.selectMyAttendance(query);
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

        // PRAFTA-SUBCON-T5: 종료도 동일 게이트로 세션에 접근한다(타사 세션 참석자의 종료 허용).
        TbmSessionQuery query = viewableQuery(sessionCd, cmpnyCd, siteCd, userCd);

        TbmSessionResult session = appTbm01Mapper.selectSession(query);
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
        TbmAttendanceResult attendance = appTbm01Mapper.selectMyAttendance(query);
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

        // 비밀번호 검증.
        verifyPassword(PWD_TYPE_EXIT, session.getExitPwd(), param.exitPwd());

        // 종료 서명 파일 저장 → EXIT_SIGN_FILE_MGMT_CD.
        // N4: 서명 파일은 <b>참석자 회사</b> 소유 데이터다. 사업장코드로 세션(개설사) SITE_CD 를 넘기면
        //   타사 테넌트의 파일 메타/경로에 개설사 사업장코드가 혼입된다 → 본인 토큰의 사업장을 쓴다
        //   (자사 세션이면 값이 동일하므로 회귀 없음. 토큰에 사업장이 없으면 saveSignatureFile 이 회사코드로 폴백).
        String signFileMgmtCd = saveSignatureFile(cmpnyCd, userCd, siteCd, signFile);

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
    // prafta-app-tbm: 사용자 앱 TBM 허브 조회/액션 (A1~A10)
    // -------------------------------------------------------------------------
    // [머지 com-007] STATUS_IN_PROGRESS/STATUS_COMPLETED 중복 선언 제거 — 클래스 상단(85~88행) 정의 재사용.

    // A1/A2/A3: 탭별 세션 리스트
    @Override
    public TbmSessionListResponse selectSessions(TbmSessionListParam param) {

        TokenInfo token = param.tokenInfo();
        TbmSessionListQuery query = TbmSessionListQuery.from(
                token.gv_cmpnyCd(), token.gv_siteCd(), token.gv_userCd());

        List<TbmSessionListResult> rows;
        switch (param.tab()) {
            case TbmSessionListParam.TAB_AVAILABLE:
                rows = appTbm01Mapper.selectAvailableSessions(query);
                break;
            case TbmSessionListParam.TAB_IN_PROGRESS:
                rows = appTbm01Mapper.selectInProgressSessions(query);
                break;
            case TbmSessionListParam.TAB_COMPLETED:
                rows = appTbm01Mapper.selectCompletedSessions(query);
                break;
            default:
                // Param.from 에서 이미 검증하지만 방어적으로 거부.
                throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        List<TbmSessionListResponse.Item> items = new ArrayList<>();
        for (TbmSessionListResult r : rows) {
            items.add(TbmSessionListResponse.Item.builder()
                    .sessionCd(r.getSessionCd())
                    .title(r.getTitle())
                    .managerUserNm(r.getManagerUserNm())
                    .openedAt(r.getOpenedAt())
                    .startedAt(r.getStartedAt())
                    .endedAt(r.getEndedAt())
                    .completionStatusCd(r.getCompletionStatusCd())
                    // PRAFTA-SUBCON-T5: 타사(연동) 세션이면 개최사 라벨(= 나를 지정한 직상위 회사).
                    //   자사 세션은 SQL 이 NULL 을 내려주므로 앱 카드에 배지가 뜨지 않는다(기존 UI 무변화).
                    .hostCmpnyNm(r.getHostCmpnyNm())
                    .build());
        }

        log.info("[tbm01] 세션 리스트 조회: tab={}, count={}", param.tab(), items.size());
        return TbmSessionListResponse.builder().sessions(items).build();
    }

    // A4: 참석자 리스트(PII 최소)
    @Override
    public TbmAttendeeListResponse selectAttendees(TbmSessionDetailParam param) {

        TbmDetailQuery query = toDetailQuery(param);
        List<TbmAttendeeResult> rows = appTbm01Mapper.selectSessionAttendees(query);

        List<TbmAttendeeListResponse.Item> items = new ArrayList<>();
        for (TbmAttendeeResult r : rows) {
            items.add(TbmAttendeeListResponse.Item.builder()
                    .userNm(r.getUserNm())
                    .entryAt(r.getEntryAt())
                    .build());
        }

        log.info("[tbm01] 참석자 리스트 조회: sessionCd={}, count={}", param.sessionCd(), items.size());
        return TbmAttendeeListResponse.builder().attendees(items).build();
    }

    // A5: 세션 상태(STATUS_CD 판정)
    @Override
    public TbmSessionStateResponse selectState(TbmSessionDetailParam param) {

        TbmDetailQuery query = toDetailQuery(param);
        TbmSessionStateResult state = appTbm01Mapper.selectSessionState(query);
        if (state == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        log.info("[tbm01] 세션 상태 조회: sessionCd={}, statusCd={}", param.sessionCd(), state.getStatusCd());
        return TbmSessionStateResponse.builder()
                .statusCd(state.getStatusCd())
                .startedAt(state.getStartedAt())
                .endedAt(state.getEndedAt())
                .syncStateCd(state.getSyncStateCd())
                .build();
    }

    // A6: 교육내용 + 자료 묶음(≤3)
    @Override
    public TbmContentResponse selectContent(TbmSessionDetailParam param) {

        TbmDetailQuery query = toDetailQuery(param);

        // 세션 본문(존재 확인 겸). 세션 없으면 본문 null → 404.
        String contentBody = appTbm01Mapper.selectSessionContentBody(query);
        if (contentBody == null && appTbm01Mapper.selectSessionState(query) == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        List<TbmSessionContentResult> mtrls = appTbm01Mapper.selectSessionContents(query);
        List<TbmContentItemResult> items = appTbm01Mapper.selectSessionContentItems(query);

        // 묶음코드별 항목 그룹핑(조회 순서=DISPLAY_ORDER, SORT_IDX 유지).
        // PRAFTA-SUBCON-T5 F8: 교육자료 파일은 <b>개설사 소유</b>(/uploads/{개설사}/...)다. 서명 payload 에
        //   뷰어 회사코드를 넣으면 FileServingFilter 의 경로-회사 검증과 불일치한다(file.sign.enforce=true
        //   전환 시 타사 세션 자료 미리보기가 전부 403). 게이트가 돌려준 개설사로 서명한다.
        String cmpnyCd = query.sessionCmpnyCd();
        Map<String, List<TbmContentResponse.Item>> itemsByMtrl = new LinkedHashMap<>();
        for (TbmContentItemResult it : items) {
            itemsByMtrl.computeIfAbsent(it.getMtrlCd(), k -> new ArrayList<>())
                    .add(TbmContentResponse.Item.builder()
                            .mtrlItemCd(it.getMtrlItemCd())
                            .type(it.getItemType())
                            .fileMgmtCd(it.getFileMgmtCd())
                            .url(it.getUrl())
                            .itemDesc(it.getItemDesc())
                            .sortIdx(it.getSortIdx())
                            // 서명 URL 전환: 파일형 항목은 서명 절대 URL(previewUrl) 발급(파일 없으면 NULL).
                            .previewUrl(signPreview(it.getFilePath(), it.getFileMgmtCd(), it.getFileExt(), cmpnyCd))
                            .build());
        }

        List<TbmContentResponse.Material> materials = new ArrayList<>();
        for (TbmSessionContentResult m : mtrls) {
            materials.add(TbmContentResponse.Material.builder()
                    .mtrlCd(m.getMtrlCd())
                    .title(m.getTitle())
                    .overrideDesc(m.getOverrideDesc())
                    .displayOrder(m.getDisplayOrder())
                    .items(itemsByMtrl.getOrDefault(m.getMtrlCd(), new ArrayList<>()))
                    .build());
        }

        log.info("[tbm01] 교육 콘텐츠 조회: sessionCd={}, materialCount={}", param.sessionCd(), materials.size());
        return TbmContentResponse.builder()
                // 저장형 XSS 차단: 관리자 입력 리치 HTML 을 응답 직전 Jsoup Safelist 로 정화.
                .contentBody(sanitizeContentBody(contentBody))
                .materials(materials)
                .build();
    }

    // A7: 연계 위험성평가 리스트
    @Override
    public TbmRiskListResponse selectRisks(TbmSessionDetailParam param) {

        TbmDetailQuery query = toDetailQuery(param);
        List<TbmSessionRiskResult> rows = appTbm01Mapper.selectSessionRisks(query);

        List<TbmRiskListResponse.Item> items = new ArrayList<>();
        for (TbmSessionRiskResult r : rows) {
            items.add(TbmRiskListResponse.Item.builder()
                    .displayName(buildRiskDisplayName(r))
                    .processNm(r.getProcessNm())
                    .riskTypeNm(r.getRiskTypeNm())
                    .hazardNm(r.getHazardNm())
                    .assessmentStatus(r.getAssessmentStatus())
                    .assessmentStatusNm(r.getAssessmentStatusNm())
                    .displayOrder(r.getDisplayOrder())
                    .build());
        }

        log.info("[tbm01] 위험성평가 리스트 조회: sessionCd={}, count={}", param.sessionCd(), items.size());
        return TbmRiskListResponse.builder().risks(items).build();
    }

    // A8: 시작전 퇴실(출결 물리 삭제, 멱등)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbmActionResponse leaveBefore(TbmSessionDetailParam param) {

        TokenInfo token = param.tokenInfo();
        // 세션 사업장 스코프 검증(타 사업장 세션 출결 조작 차단).
        TbmDetailQuery query = toDetailQuery(param);
        if (appTbm01Mapper.selectSessionState(query) == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        int deleted = appTbm01Mapper.deleteAttendance(
                TbmLeaveBeforeCommand.of(token.gv_cmpnyCd(), param.sessionCd(), token.gv_userCd()));

        // 멱등: 이미 취소(또는 종료되어 미종료 행 없음)면 무해 응답.
        boolean alreadyProcessed = deleted == 0;
        log.info("[tbm01] 시작전 퇴실: sessionCd={}, deleted={}", param.sessionCd(), deleted);
        return TbmActionResponse.builder()
                .success(true)
                .alreadyProcessed(alreadyProcessed)
                .build();
    }

    // A9: 중도퇴실(미이수 종료, 멱등)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbmActionResponse withdraw(TbmSessionDetailParam param) {

        TokenInfo token = param.tokenInfo();
        // 세션 사업장 스코프 검증.
        TbmDetailQuery query = toDetailQuery(param);
        if (appTbm01Mapper.selectSessionState(query) == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        int updated = appTbm01Mapper.updateWithdraw(
                TbmWithdrawCommand.of(token.gv_cmpnyCd(), param.sessionCd(), token.gv_userCd()));

        // 멱등: 이미 종료된 경우(미종료 행 없음) 무해 응답.
        boolean alreadyProcessed = updated == 0;
        log.info("[tbm01] 중도퇴실: sessionCd={}, updated={}", param.sessionCd(), updated);
        return TbmActionResponse.builder()
                .success(true)
                .alreadyProcessed(alreadyProcessed)
                .build();
    }

    // A10: 완료 상세
    @Override
    public TbmCompletionResponse selectMyCompletion(TbmSessionDetailParam param) {

        TbmDetailQuery query = toDetailQuery(param);

        TbmCompletionResult head = appTbm01Mapper.selectMyCompletion(query);
        if (head == null) {
            // 본인 출결 이력이 없으면 완료 상세 없음.
            throw new ApiException(TbmErrorCode.TBM_404_020);
        }

        List<String> materialTitles = appTbm01Mapper.selectSessionMaterialTitles(query);

        // 위험성 제목(displayName) 목록만 추출.
        List<TbmSessionRiskResult> risks = appTbm01Mapper.selectSessionRisks(query);
        List<String> riskTitles = new ArrayList<>();
        for (TbmSessionRiskResult r : risks) {
            riskTitles.add(buildRiskDisplayName(r));
        }

        log.info("[tbm01] 완료 상세 조회: sessionCd={}, completionStatusCd={}",
                param.sessionCd(), head.getCompletionStatusCd());
        return TbmCompletionResponse.builder()
                .title(head.getTitle())
                // 저장형 XSS 차단: 관리자 입력 리치 HTML 을 응답 직전 Jsoup Safelist 로 정화.
                .contentBody(sanitizeContentBody(head.getContentBody()))
                .materialTitles(materialTitles)
                .riskTitles(riskTitles)
                .mySignFileMgmtCd(head.getMySignFileMgmtCd())
                // security H-1(SEC-A): 서명 이미지의 공개 정적 URL 발급을 중단한다(항상 NULL).
                //   종전에는 정적 URL 을 내려줬으나, 그 URL 하나로 공개 디렉토리·일자·순차 파일코드가
                //   전부 확정돼 무인증 열거의 진입점이 됐다(구 '003' 저장분은 소급 이전 없이 공개 경로에 잔존).
                //   열람은 인증 스트림 EP(GET /appApi/tbm/my-sign-image, 본인 한정)로만 한다.
                //   필드 자체는 폴백 번들 호환을 위해 남기되 값은 항상 NULL 이다.
                .mySignUrl(null)
                .completionStatusCd(head.getCompletionStatusCd())
                .endedAt(head.getEndedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // A10-1: 본인 종료 서명 이미지 스트림 (security H-1 후속, 2026-08-31)
    // -------------------------------------------------------------------------

    /**
     * 본인 종료 서명 이미지 스트림.
     *
     * <p>서명 파일타입이 보호 타입('009')으로 전환되면서 공개 정적 URL(/uploads/**) 로는 열람할 수 없다.
     * 웹 W-13 {@code attendance-sign-image} / {@code manager-sign-image} 패턴 미러 —
     * 공개 정적 URL 금지, 인증 스트림 서빙, 파일 식별자는 서버가 출결 행에서 재조회(클라 파일코드 신뢰 금지).
     *
     * <p>인가: A10(완료 상세)과 완전히 동일한 경로 — 세션 접근 게이트({@code assertViewable}) 통과 후
     * 본인 회사/본인 사용자/REGULAR 스코프의 출결 행만 조회한다(매퍼 WHERE 가 USER_CD 를 강제하므로
     * 타인 서명에는 도달할 수 없다). 대상 없음은 존재 비노출 통합 404(TBM_404_020).
     *
     * <p>기존 '003' 으로 저장된 서명 파일도 이 EP 로 열람된다 — {@code FileServiceImpl.resolveSavePath}
     * 가 DB FILE_PATH 선두 프리픽스로 base 를 판별하므로 공개/보호 저장분을 모두 읽는다(별도 분기 불필요).
     */
    @Override
    public com.prafta.common.cmm.file.application.model.FileBytesResult loadMySignImage(TbmSessionDetailParam param) {

        // 세션 접근 게이트 + 개설사/내 회사 키 확정(A10 과 동일).
        TbmDetailQuery query = toDetailQuery(param);

        TbmCompletionResult head = appTbm01Mapper.selectMyCompletion(query);
        if (head == null || !StringUtils.hasText(head.getMySignFileMgmtCd())) {
            // 본인 출결 없음 또는 서명 미등록 — 존재 비노출 통합 404.
            throw new ApiException(TbmErrorCode.TBM_404_020);
        }

        // 서명 파일은 참석자(본인) 회사 소유 데이터다 → 내 회사코드로 로드(타사 세션이어도 동일).
        com.prafta.common.cmm.file.application.model.FileBytesResult file = fileService.loadFileBytes(
                new com.prafta.common.cmm.file.application.query.FileReadQuery(
                        query.cmpnyCd(), head.getMySignFileMgmtCd()));
        if (file == null) {
            // DB 행/디스크 원본 부재 — 존재 비노출 통합 404.
            throw new ApiException(TbmErrorCode.TBM_404_020);
        }

        log.info("[tbm01] 본인 서명 이미지 스트림: sessionCd={}", param.sessionCd());
        return file;
    }

    // [정합성 수정] 본인 출결 상태 조회(대기/진행 화면 이탈 감지용).
    //   present=false → 관리자 내보내기(cancel-entry)로 삭제됨. exitTypeCd='MANAGER_FORCED' → 강제퇴실.
    //   스코프는 JWT(userCd)만 신뢰하며, 세션 사업장 스코프(selectSession)로 타 사업장 접근을 차단한다.
    @Override
    public TbmMyAttendanceResponse selectMyAttendanceStatus(TbmSessionDetailParam param) {

        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();
        String userCd = token.gv_userCd();
        String sessionCd = param.sessionCd();

        // 세션 접근 게이트(자사=사업장 스코프, 타사=지정 체인). 없으면 404.
        TbmSessionQuery query = viewableQuery(sessionCd, cmpnyCd, siteCd, userCd);

        TbmSessionResult session = appTbm01Mapper.selectSession(query);
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }

        TbmAttendanceResult attendance = appTbm01Mapper.selectMyAttendance(query);

        boolean present = attendance != null;
        boolean entered = present && attendance.getEntryAt() != null;

        log.info("[tbm01] 본인 출결 상태 조회: sessionCd={}, present={}, entered={}", sessionCd, present, entered);
        return TbmMyAttendanceResponse.builder()
                .present(present)
                .entered(entered)
                .exitAt(present ? formatDate(attendance.getExitAt()) : null)
                .exitTypeCd(present ? attendance.getExitTypeCd() : null)
                .completionStatusCd(present ? attendance.getCompletionStatusCd() : null)
                .build();
    }

    // prafta-app-022-6: 퇴근 시 본인 진행중 TBM 자동 중도퇴실(attd01 → tbm01 단방향 호출).
    //   @Transactional 미부여(의도): 호출부(checkOut)의 트랜잭션(REQUIRED)에 참여하여 퇴근과 함께
    //   커밋된다. 자체 트랜잭션 경계를 두지 않으므로, 본 메서드에서 예외가 나도 트랜잭션이
    //   rollback-only 로 강제 마킹되지 않는다 → 호출부 try-catch 가 예외를 삼켜 퇴근 커밋을 보장한다.
    @Override
    public int withdrawAllInProgress(TokenInfo token) {

        if (token == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // 본인+REGULAR+사업장 스코프로 진행중(입실O·미종료) 세션 식별.
        TbmSessionListQuery query = TbmSessionListQuery.from(
                token.gv_cmpnyCd(), token.gv_siteCd(), token.gv_userCd());

        List<String> sessionCds = appTbm01Mapper.selectInProgressAttendanceSessionCds(query);
        if (sessionCds == null || sessionCds.isEmpty()) {
            // 진행중 세션 없음 → no-op(멱등).
            return 0;
        }

        int processed = 0;
        for (String sessionCd : sessionCds) {
            // 기존 중도퇴실 SQL 재사용(EXIT_TYPE_CD='SELF', COMPLETION_STATUS_CD='NOT_COMPLETED').
            //   본인+미종료만 갱신되므로 동시성/중복 호출에도 멱등.
            int updated = appTbm01Mapper.updateWithdraw(
                    TbmWithdrawCommand.of(token.gv_cmpnyCd(), sessionCd, token.gv_userCd()));
            if (updated > 0) {
                processed++;
            }
        }

        log.info("[tbm01] 퇴근 연동 자동 중도퇴실: userCd={}, target={}, processed={}",
                token.gv_userCd(), sessionCds.size(), processed);
        return processed;
    }

    /**
     * 저장형 XSS 방어: 교육내용 리치 HTML(contentBody) 을 응답 직전 정화한다.
     *
     * <p>관리자가 web tbm02 에서 입력한 contentBody 는 a/img/표/서식 등 리치 HTML 서식을
     * 보존해야 하므로 {@link Safelist#relaxed()} 를 기준으로 한다. relaxed() 는 본문 서식
     * 태그와 a/img(및 표) 를 허용하되, {@code <script>}·{@code onerror}/{@code onclick} 등
     * 이벤트 핸들러 속성·{@code javascript:} 스킴 링크는 자동으로 제거한다.
     *
     * <p>A6(content)·A10(my-completion) 앱 읽기 경로 양쪽에서 재사용한다.
     * null/blank 는 그대로 반환한다(불필요한 정화 호출 방지).
     */
    private String sanitizeContentBody(String body) {
        if (!StringUtils.hasText(body)) {
            return body;
        }
        return Jsoup.clean(body, Safelist.relaxed());
    }

    /**
     * 파일형 항목의 서명 미리보기 URL 발급.
     * <p>relPath = FILE_PATH + '/' + FILE_MGMT_CD + FILE_EXT(기존 CONCAT 정합). 파일 없으면(filePath blank) NULL.
     */
    private String signPreview(String filePath, String fileMgmtCd, String fileExt, String cmpnyCd) {
        if (!StringUtils.hasText(filePath) || !StringUtils.hasText(fileMgmtCd)) {
            return null;
        }
        String relPath = filePath + "/" + fileMgmtCd + (fileExt != null ? fileExt : "");
        return fileUrlSigner.sign(relPath, cmpnyCd);
    }

    /**
     * A4~A10 공용: Param(token) → Query 변환.
     *
     * <p>PRAFTA-SUBCON-T5: 사업장 WHERE 강제 대신 공통 게이트({@code assertViewable})가 접근을
     * 판정한다(자사 세션은 사업장 스코프 유지 — 게이트 내부에서 검사, 타사 세션은 지정 체인 도달성).
     * 게이트가 돌려준 개설사(sessionCmpnyCd)로 세션/콘텐츠/위험성/자료를 조회하고, 출결은 내 회사로 조회한다.
     */
    private TbmDetailQuery toDetailQuery(TbmSessionDetailParam param) {
        TokenInfo token = param.tokenInfo();
        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();

        // M4: grandfather(지정 해제 후 기존 참석자 조회 허용) 판정은 사용자 단위 → 본인 식별자를 넘긴다.
        TbmSessionAccess access = tbmSessionShareService.assertViewable(
                param.sessionCd(), cmpnyCd, siteCd, USER_TYPE_REGULAR, token.gv_userCd());

        return TbmDetailQuery.of(
                cmpnyCd, access.hostCmpnyCd(), siteCd, param.sessionCd(), token.gv_userCd());
    }

    /** 입실/종료/컨텍스트 공용: 게이트 통과 후 세션 키(개설사)/출결 키(내 회사)를 담은 Query 생성. */
    private TbmSessionQuery viewableQuery(String sessionCd, String cmpnyCd, String siteCd, String userCd) {
        TbmSessionAccess access = tbmSessionShareService.assertViewable(
                sessionCd, cmpnyCd, siteCd, USER_TYPE_REGULAR, userCd);
        return TbmSessionQuery.of(cmpnyCd, access.hostCmpnyCd(), siteCd, sessionCd, userCd);
    }

    /** A7/A10: 위험성 displayName 합성(공정 · 유형 · 유해위험요인). web SessionRiskItem.displayName 규칙. */
    private String buildRiskDisplayName(TbmSessionRiskResult r) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(r.getProcessNm())) parts.add(r.getProcessNm());
        if (StringUtils.hasText(r.getRiskTypeNm())) parts.add(r.getRiskTypeNm());
        if (StringUtils.hasText(r.getHazardNm())) parts.add(r.getHazardNm());
        return String.join(" · ", parts);
    }

    // -------------------------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------------------------

    /**
     * 비밀번호 검증(불일치 시 TBM_400_030).
     * <p>입실/종료 비밀번호는 현장에서 공유받는 passcode 이므로 시도 횟수 제한(잠금)을 두지 않는다.
     * 맞는 비밀번호로만 입실/종료가 가능하며, 틀려도 잠금 없이 재시도할 수 있다.
     */
    private void verifyPassword(String pwdTypeCd, String storedPwd, String inputPwd) {

        boolean match = StringUtils.hasText(storedPwd) && storedPwd.equals(inputPwd);
        if (!match) {
            log.info("[tbm01] 비밀번호 불일치: type={}", pwdTypeCd);
            throw new ApiException(TbmErrorCode.TBM_400_030);
        }
    }

    /**
     * D5: 거리 계산 및 검증.
     * <p>AUTO: 좌표/세션좌표로 거리 산출, 반경 초과 시 차단. MANUAL/DISABLED: 거리만 기록(차단 안 함).
     * <p>GPS좌표-암호화-전환-06: 세션 저장 좌표는 fallback 복호화(ENC 우선, NULL 이면 구 평문)로
     * 확정한 뒤 기존 haversine 계산에 사용한다 — 판정 로직/결과는 전환 전후 동일.
     * 복호화 좌표값은 로그에 출력하지 않는다(D5).
     * @return 거리(m). 계산 불가(좌표/세션좌표 부재)면 null.
     */
    private Integer resolveDistanceAndVerify(TbmSessionResult session, Double lat, Double lon) {

        String verifyType = session.getGpsVerifyTypeCd();

        // DISABLED: 검증 생략(거리 미산출).
        if ("DISABLED".equals(verifyType)) {
            return null;
        }

        // 세션 좌표 fallback resolve(-06): 암호문이 있으면 복호화값, 없으면 구 평문(백필 전 행).
        java.math.BigDecimal sessionLat =
                gpsCoordCrypto.resolveToBigDecimal(session.getManagerGpsLatEnc(), session.getManagerGpsLat());
        java.math.BigDecimal sessionLon =
                gpsCoordCrypto.resolveToBigDecimal(session.getManagerGpsLonEnc(), session.getManagerGpsLon());

        Integer distanceM = null;
        if (lat != null && lon != null
                && sessionLat != null && sessionLon != null) {
            double d = haversineMeters(
                    lat, lon,
                    sessionLat.doubleValue(),
                    sessionLon.doubleValue());
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
