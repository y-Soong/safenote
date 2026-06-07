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
import com.prafta.app.tbm.tbm01.dto.response.TbmRiskListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionStateResponse;
import com.prafta.app.tbm.tbm01.mapper.AppTbm01Mapper;
import com.prafta.app.tbm.tbm01.result.TbmAttendanceResult;
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
import com.prafta.common.cmm.worktime.service.WorktimeGateService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.FileUrlSigner;

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

    /** TB_FILE_INFO.FILE_TYPE — 003: TBM 서명(디렉토리 그룹). */
    private static final String FILE_TYPE_TBM_SIGN = "003";

    private static final String STATUS_OPENED = "OPENED";
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

        // prafta-app-022: 근무중 게이트 — 근무 중에만 TBM 입실 허용(정책 safety §2).
        //   세션 존재 확인(404) 직후, OPENED 검증/비번 검증/멱등 분기보다 앞에서 차단한다.
        //   exit/leaveBefore/withdraw 에는 적용하지 않는다(입실만 게이트).
        worktimeGateService.assertWorking(token);

        // D3: OPENED 일 때만 입실 허용.
        if (!STATUS_OPENED.equals(session.getStatusCd())) {
            log.info("[tbm01] 입실 불가 상태: sessionCd={}, status={}", sessionCd, session.getStatusCd());
            throw new ApiException(TbmErrorCode.TBM_409_030);
        }

        // 비밀번호 검증. 반드시 기입실 멱등 분기보다 먼저 수행한다. 비번 검증을 멱등 분기 뒤에 두면
        // 1회 입실(종료 전) 이후에는 잘못된 비밀번호로도 멱등 응답으로 입실 통과되는
        // 검증 우회가 발생한다(맞는 비밀번호로만 입실 허용).
        verifyPassword(PWD_TYPE_ENTRY, session.getEntryPwd(), param.entryPwd());

        // 기입실(멱등): 비밀번호가 일치한 경우에만, UNIQUE 충돌 전에 선조회로 빠르게 안내.
        TbmAttendanceResult existing = appTbm01Mapper.selectMyAttendance(
                TbmSessionQuery.from(cmpnyCd, siteCd, sessionCd, userCd));
        if (existing != null && existing.getEntryAt() != null) {
            return idempotentEnterResponse(existing);
        }

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

        // 비밀번호 검증.
        verifyPassword(PWD_TYPE_EXIT, session.getExitPwd(), param.exitPwd());

        // 종료 서명 파일 저장 → EXIT_SIGN_FILE_MGMT_CD.
        String signFileMgmtCd = saveSignatureFile(cmpnyCd, userCd, session.getSiteCd(), signFile);

        // 출결 UPDATE(본인+미종료만).
        TbmExitCommand command = TbmExitCommand.of(cmpnyCd, sessionCd, userCd, signFileMgmtCd);
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

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

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
        String cmpnyCd = param.tokenInfo().gv_cmpnyCd();
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
                // Q6 플래그: 파일코드→이미지 URL 변환 컨트롤러 부재 → 파일코드 원본만 응답(web tbm04 동일).
                .mySignFileMgmtCd(head.getMySignFileMgmtCd())
                .completionStatusCd(head.getCompletionStatusCd())
                .endedAt(head.getEndedAt())
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

    /** A4~A10 공용: Param(token) → 사업장 스코프 강제 Query 변환. */
    private TbmDetailQuery toDetailQuery(TbmSessionDetailParam param) {
        TokenInfo token = param.tokenInfo();
        return TbmDetailQuery.from(
                token.gv_cmpnyCd(), token.gv_siteCd(), param.sessionCd(), token.gv_userCd());
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
