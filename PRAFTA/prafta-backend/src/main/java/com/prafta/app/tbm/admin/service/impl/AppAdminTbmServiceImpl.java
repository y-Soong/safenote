package com.prafta.app.tbm.admin.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.admin.access.application.query.AdminAccessQuery;
import com.prafta.app.admin.access.mapper.AppAdminAccessMapper;
import com.prafta.app.admin.access.result.AccessibleSiteResult;
import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.tbm.admin.application.command.AdminCompletionCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialItemCommand;
import com.prafta.app.tbm.admin.application.command.AdminForceExitCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCancelCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionContentCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionPwdCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionRiskCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionStateCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionTransitionCommand;
import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;
import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;
import com.prafta.app.tbm.admin.application.param.AdminAttendeeListParam;
import com.prafta.app.tbm.admin.application.param.AdminCompletionParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialListParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminForceExitParam;
import com.prafta.app.tbm.admin.application.param.AdminHistoryListParam;
import com.prafta.app.tbm.admin.application.param.AdminLiveTransitionParam;
import com.prafta.app.tbm.admin.application.param.AdminOptionParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionCancelParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionListParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPwdParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionUpdateParam;
import com.prafta.app.tbm.admin.application.query.AdminAttendeeListQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialListQuery;
import com.prafta.app.tbm.admin.application.query.AdminHistoryListQuery;
import com.prafta.app.tbm.admin.application.query.AdminOptionQuery;
import com.prafta.app.tbm.admin.application.query.AdminSessionDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminSessionListQuery;
import com.prafta.app.tbm.admin.dto.response.AdminAttendeeListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminCompletionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminContentOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminForceExitResponse;
import com.prafta.app.tbm.admin.dto.response.AdminHistoryListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminLiveTransitionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminMaterialTypeOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminRiskOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionContentsResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionPwdResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSiteOptionResponse;
import com.prafta.app.tbm.admin.mapper.AppAdminTbmMapper;
import com.prafta.app.tbm.admin.result.AdminAttendeeResult;
import com.prafta.app.tbm.admin.result.AdminContentItemResult;
import com.prafta.app.tbm.admin.result.AdminContentOptionResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialItemResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialListResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialResult;
import com.prafta.app.tbm.admin.result.AdminHistoryListResult;
import com.prafta.app.tbm.admin.result.AdminHistoryStatResult;
import com.prafta.app.tbm.admin.result.AdminMaterialTypeOptionResult;
import com.prafta.app.tbm.admin.result.AdminRiskOptionResult;
import com.prafta.app.tbm.admin.result.AdminSessionContentResult;
import com.prafta.app.tbm.admin.result.AdminSessionGuardResult;
import com.prafta.app.tbm.admin.result.AdminSessionListResult;
import com.prafta.app.tbm.admin.result.AdminSessionResult;
import com.prafta.app.tbm.admin.result.AdminSessionRiskResult;
import com.prafta.app.tbm.admin.service.AppAdminTbmService;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.FileUrlSigner;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.NumericPwdGenerator;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialItemResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 001-P5: 앱 관리자 TBM 관리 서비스 구현(R1+R2 — 교육관리/상세/개설/수정/취소/비번재발급/옵션).
 *
 * <p>권한([권한매트릭스 §3]): TBM = master ∥ safe ∥ nodeAdmin (hr 단독 ⛔).
 * <p>스코프([권한매트릭스 §2] / plan §6·T7): master/safe=전사. 노드관리자=접근가능 사업장
 *   AND 개설자(MANAGER_USER_CD) NODE_CD ∈ scopedNodeCds(자기노드+자손, Phase 1 재귀 CTE 재사용).
 * <p>식별자는 모두 JWT 클레임에서만 도출(D1/IDOR 차단). path sessionCd 는 토큰 스코프로 재검증.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminTbmServiceImpl implements AppAdminTbmService {

    private final AppAdminTbmMapper appAdminTbmMapper;
    private final AppAdminAccessMapper appAdminAccessMapper;   // 접근가능 사업장(access-context 동일 소스)
    private final AdminScopeMapper adminScopeMapper;           // 노드 자손 전개(재귀 CTE)
    private final FileService fileService;                     // R5 자료 파일 저장(공통 인프라)
    private final FileMapper fileMapper;                       // R5 자료 파일코드 채번/메타
    private final FileUrlSigner fileUrlSigner;                 // 파일 서빙 서명 URL 발급(공통 인프라)

    private static final int PWD_LENGTH = 6;
    private static final int GPS_RADIUS_MIN = 50;
    private static final int GPS_RADIUS_MAX = 1000;
    private static final int CONTENT_TEXT_MIN = 10;
    private static final int NOT_COMPLETED_REASON_MIN = 10;   // R3 T4 미이수 사유 최소 길이

    // ===== R5 교육자료 =====
    private static final int TITLE_MAX = 200;
    /** 자료 항목 타입(SYS018). */
    private static final String ITEM_TYPE_IMAGE = "01";
    private static final String ITEM_TYPE_VIDEO = "02";
    private static final String ITEM_TYPE_URL = "03";
    private static final String ITEM_TYPE_PDF = "04";
    /** TB_FILE_INFO.FILE_TYPE — '003' TBM 교육자료(web Tbm01 정합, 디렉토리 그룹). */
    private static final String FILE_TYPE_TBM_MTRL = "003";
    /** 업로드 파일 크기 상한(50MB — 동영상/PDF 허용). */
    private static final long MTRL_FILE_MAX_BYTES = 50L * 1024 * 1024;
    /** 업로드 허용 확장자(이미지/동영상/PDF). FileServiceImpl 화이트리스트와 별개의 도메인 한정 검증. */
    private static final java.util.Set<String> MTRL_ALLOWED_EXTS = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "mp4", "mov", "avi", "mkv", "webm", "m4v",
            "pdf");
    /** 이미지 확장자 그룹(content-type/매직바이트 정합 분기용). */
    private static final java.util.Set<String> IMAGE_EXTS = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp");
    /** 매직바이트 판정용 선두 프로브 길이(WEBP RIFF....WEBP 까지 커버). */
    private static final int MAGIC_PROBE_BYTES = 16;
    /** 외부링크(항목타입 03) URL 최대 길이(컬럼 정합). */
    private static final int URL_MAX_LEN = 1000;

    private static final String RISK_WARNING_MESSAGE =
        "위험성평가 연동되지 않은 TBM입니다. 사고 발생 시 설득력이 떨어질 수 있습니다.";

    // ============================ 스코프 산출(공통) ============================

    /**
     * 관리자 진입/스코프 컨텍스트(전사 여부 + 접근가능 사업장 + 노드 자손 집합)를 산출한다.
     * <p>master/safe = companyWide(전사). 그 외(노드관리자/hr 등)는 접근가능 사업장과 scopedNodeCds 로 제한하며,
     * 둘 다 비면(노드관리자 아님 = hr 단독 등) 진입을 차단한다(hr ⛔).
     */
    private ScopeContext resolveScope(String cmpnyCd, String userCd, String siteCd, String authCd) {
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("TBM 관리 접근 차단 - authCd={}", authCd);
            throw new ApiException(TbmErrorCode.TBM_403_001);
        }

        boolean companyWide = AuthRoleUtils.isCompanyWide(authCd);   // master/safe
        if (companyWide) {
            return new ScopeContext(true, Collections.emptyList(), Collections.emptyList());
        }

        // 노드관리자 축: 현재 사업장 기준 자기노드+자손 NODE_CD 집합.
        List<String> scopedNodeCds = Collections.emptyList();
        if (StringUtils.hasText(siteCd)) {
            scopedNodeCds = adminScopeMapper.selectScopedNodeCds(
                    ScopedNodeQuery.of(cmpnyCd, siteCd, userCd));
        }
        // 접근가능 사업장(USE_YN='Y') — access-context 와 동일 소스.
        List<AccessibleSiteResult> sites = appAdminAccessMapper.selectAccessibleSites(
                AdminAccessQuery.ofCompany(cmpnyCd, userCd));
        List<String> siteCds = new ArrayList<>();
        if (sites != null) {
            for (AccessibleSiteResult s : sites) {
                siteCds.add(s.getSiteCd());
            }
        }

        // 노드관리자가 아니면(scopedNodeCds 비어 있음) 진입 불가 — hr 단독 ⛔ 포함.
        if (scopedNodeCds == null || scopedNodeCds.isEmpty()) {
            log.warn("TBM 관리 진입 권한 없음(노드관리자 아님) - authCd={}, siteCd={}", authCd, siteCd);
            throw new ApiException(TbmErrorCode.TBM_403_010);
        }

        return new ScopeContext(false, siteCds, scopedNodeCds);
    }

    /** 스코프 컨텍스트 보관용. */
    private record ScopeContext(boolean companyWide, List<String> accessibleSiteCds, List<String> scopedNodeCds) {
    }

    // ============================ T-A1 교육관리 리스트 ============================

    @Override
    public AdminSessionListResponse selectSessionList(AdminSessionListParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        int offset = (param.page() - 1) * param.pageSize();
        AdminSessionListQuery query = new AdminSessionListQuery(
                param.statusCd(),
                param.keyword(),
                scope.companyWide(),
                scope.accessibleSiteCds(),
                scope.scopedNodeCds(),
                offset,
                param.pageSize(),
                param.gvCmpnyCd());

        List<AdminSessionListResult> list = appAdminTbmMapper.selectManageSessions(query);
        int totalCount = appAdminTbmMapper.selectManageSessionsCount(query);

        log.info("앱 관리자 TBM 교육관리 리스트 조회 완료 - cmpnyCd={}, companyWide={}, count={}, totalCount={}",
                param.gvCmpnyCd(), scope.companyWide(), list != null ? list.size() : 0, totalCount);

        return AdminSessionListResponse.builder()
                .sessions(list != null ? list : Collections.emptyList())
                .totalCount(totalCount)
                .page(param.page())
                .pageSize(param.pageSize())
                .build();
    }

    // ============================ T-A2 상세 ============================

    @Override
    public AdminSessionDetailResponse selectSessionDetail(AdminSessionDetailParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionDetailQuery query = AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd());

        AdminSessionResult session = appAdminTbmMapper.selectSessionDetail(query);
        if (session == null) {
            throw new ApiException(TbmErrorCode.TBM_404_010);
        }

        // 스코프 격리(IDOR): 노드관리자면 사업장 + 개설자 노드 모두 통과해야 열람 가능.
        // 개설자 NODE_CD 는 guard 경량 조회로 확보(세션 헤더에는 노드 컬럼이 없음).
        if (!scope.companyWide()) {
            AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
            verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        }

        List<AdminSessionContentResult> contents = appAdminTbmMapper.selectSessionContents(query);
        List<AdminSessionRiskResult> risks = appAdminTbmMapper.selectSessionRisks(query);

        boolean pwdVisible = isPwdVisible(session.statusCd(), param.gvAuthCd(), scope.companyWide());

        AdminSessionDetailResponse.SessionDetailItem item = AdminSessionDetailResponse.SessionDetailItem.builder()
                .sessionCd(session.sessionCd())
                .siteCd(session.siteCd())
                .siteNm(session.siteNm())
                .eduTypeCd(session.eduTypeCd())
                .title(session.title())
                .contentBody(session.contentBody())
                .contentFormatCd(session.contentFormatCd())
                .statusCd(session.statusCd())
                .statusNm(session.statusNm())
                .entryPwd(pwdVisible ? session.entryPwd() : null)
                .exitPwd(pwdVisible ? session.exitPwd() : null)
                .pwdVisible(pwdVisible)
                .managerUserCd(session.managerUserCd())
                .managerUserNm(session.managerUserNm())
                .managerGpsLat(session.managerGpsLat())
                .managerGpsLon(session.managerGpsLon())
                .gpsVerifyTypeCd(session.gpsVerifyTypeCd())
                .gpsVerifyRadiusM(session.gpsVerifyRadiusM())
                .gpsManualConfirmYn(session.gpsManualConfirmYn())
                .openedAt(session.openedAt())
                .startedAt(session.startedAt())
                .endedAt(session.endedAt())
                .cancelledAt(session.cancelledAt())
                .cancelReason(session.cancelReason())
                .insertNm(session.insertNm())
                .insertDate(session.insertDate())
                .build();

        List<AdminSessionDetailResponse.SessionRiskItem> riskItems = new ArrayList<>();
        if (risks != null) {
            for (AdminSessionRiskResult r : risks) {
                riskItems.add(AdminSessionDetailResponse.SessionRiskItem.builder()
                        .siteCd(r.siteCd())
                        .processCd(r.processCd())
                        .processNm(r.processNm())
                        .riskTypeCd(r.riskTypeCd())
                        .riskTypeNm(r.riskTypeNm())
                        .hazardCd(r.hazardCd())
                        .hazardNm(r.hazardNm())
                        .assessmentCd(r.assessmentCd())
                        .assessmentStatus(r.assessmentStatus())
                        .assessmentStatusNm(r.assessmentStatusNm())
                        .displayName(buildRiskDisplayName(r.processNm(), r.riskTypeNm(), r.hazardNm()))
                        .displayOrder(r.displayOrder())
                        .build());
            }
        }

        log.info("앱 관리자 TBM 세션 상세 조회 완료 - sessionCd={}, status={}, pwdVisible={}",
                session.sessionCd(), session.statusCd(), pwdVisible);

        return AdminSessionDetailResponse.builder()
                .session(item)
                .contents(contents != null ? contents : Collections.emptyList())
                .risks(riskItems)
                .build();
    }

    // ============================ T-A3 개설/임시저장 ============================

    @Override
    @Transactional
    public AdminSessionSaveResponse saveSession(AdminSessionSaveParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        boolean opened = "OPENED".equals(param.saveMode());
        if (!opened && !"DRAFT".equals(param.saveMode())) {
            log.warn("TBM 세션 저장 모드 부적합 - saveMode={}", param.saveMode());
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        // T6: 개설 사업장은 접근가능 사업장(USE_YN='Y')인지 서버 검증.
        if (!StringUtils.hasText(param.siteCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (appAdminTbmMapper.existsAccessibleSite(param.gvCmpnyCd(), param.gvUserCd(), param.siteCd()) != 1) {
            log.warn("TBM 세션 개설 사업장 권한 없음 - userCd={}, siteCd={}", param.gvUserCd(), param.siteCd());
            throw new ApiException(TbmErrorCode.TBM_403_011);
        }

        // 제목 검증
        String title = param.title() != null ? param.title().trim() : "";
        if (!StringUtils.hasText(title) || title.length() > 200) {
            throw new ApiException(TbmErrorCode.TBM_400_011);
        }

        // 개설 시: 교육 내용 텍스트 >= 10자(T5: plain text), GPS 검증
        if (opened) {
            validateContentBody(param.contentBody());
            validateGps(param.gpsVerifyTypeCd(), param.managerGpsLat(), param.managerGpsLon(),
                    param.gpsVerifyRadiusM(), param.gpsManualConfirmYn());
        }

        String sessionCd = appAdminTbmMapper.selectSessionCd(param.gvCmpnyCd());
        if (!StringUtils.hasText(sessionCd)) {
            log.error("TBM 세션코드 채번 실패 - cmpnyCd={}", param.gvCmpnyCd());
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }

        String entryPwd = null;
        String exitPwd = null;
        if (opened) {
            String[] pair = NumericPwdGenerator.generatePair(PWD_LENGTH);
            entryPwd = pair[0];
            exitPwd = pair[1];
        }

        String statusCd = opened ? "OPENED" : "DRAFT";

        appAdminTbmMapper.insertSession(
                AdminSessionCommand.forSave(param, sessionCd, statusCd, entryPwd, exitPwd, opened));

        insertContents(param.contents(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());
        insertRisks(param.risks(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());

        if (opened) {
            appAdminTbmMapper.upsertSessionState(
                    AdminSessionStateCommand.of(sessionCd, param.gvCmpnyCd(), param.gvUserCd()));
        }

        boolean riskEmpty = param.risks() == null || param.risks().isEmpty();

        log.info("앱 관리자 TBM 세션 저장 완료 - sessionCd={}, status={}, riskEmpty={}", sessionCd, statusCd, riskEmpty);

        return AdminSessionSaveResponse.builder()
                .sessionCd(sessionCd)
                .statusCd(statusCd)
                .entryPwd(entryPwd)
                .exitPwd(exitPwd)
                .warningMessage(riskEmpty ? RISK_WARNING_MESSAGE : null)
                .build();
    }

    // ============================ T-A4 수정 ============================

    @Override
    @Transactional
    public void updateSession(AdminSessionUpdateParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        if (!"DRAFT".equals(guard.statusCd()) && !"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 세션 수정 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_010);
        }

        String title = param.title() != null ? param.title().trim() : "";
        if (!StringUtils.hasText(title) || title.length() > 200) {
            throw new ApiException(TbmErrorCode.TBM_400_011);
        }

        if ("OPENED".equals(guard.statusCd())) {
            validateContentBody(param.contentBody());
            validateGps(param.gpsVerifyTypeCd(), param.managerGpsLat(), param.managerGpsLon(),
                    param.gpsVerifyRadiusM(), param.gpsManualConfirmYn());
        }

        appAdminTbmMapper.updateSession(AdminSessionCommand.forUpdate(param));

        appAdminTbmMapper.deleteSessionContents(param.gvCmpnyCd(), param.sessionCd());
        insertContents(param.contents(), param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd());

        appAdminTbmMapper.deleteSessionRisks(param.gvCmpnyCd(), param.sessionCd());
        insertRisks(param.risks(), param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd());

        log.info("앱 관리자 TBM 세션 수정 완료 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
    }

    // ============================ T-A4 취소 ============================

    @Override
    @Transactional
    public void cancelSession(AdminSessionCancelParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (!StringUtils.hasText(param.cancelReason())) {
            throw new ApiException(TbmErrorCode.TBM_400_014);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        if (!"DRAFT".equals(guard.statusCd()) && !"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 세션 취소 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_011);
        }

        appAdminTbmMapper.cancelSession(AdminSessionCancelCommand.from(param));

        log.info("앱 관리자 TBM 세션 취소 완료 - sessionCd={}, prevStatus={}", param.sessionCd(), guard.statusCd());
    }

    // ============================ T-A4 비밀번호 재발급 ============================

    @Override
    @Transactional
    public AdminSessionPwdResponse regeneratePassword(AdminSessionPwdParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        if (!"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 세션 비번 재발급 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_012);
        }

        String[] pair = NumericPwdGenerator.generatePair(PWD_LENGTH);
        appAdminTbmMapper.updateSessionPwd(AdminSessionPwdCommand.of(
                param.sessionCd(), pair[0], pair[1], param.gvCmpnyCd(), param.gvUserCd()));

        log.info("앱 관리자 TBM 세션 비번 재발급 완료 - sessionCd={}", param.sessionCd());

        return AdminSessionPwdResponse.builder()
                .sessionCd(param.sessionCd())
                .entryPwd(pair[0])
                .exitPwd(pair[1])
                .build();
    }

    // ============================ T-K 보조 옵션 ============================

    @Override
    public AdminContentOptionResponse selectContentOptions(AdminOptionParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        AdminOptionQuery query = new AdminOptionQuery(
                param.siteCd(), param.searchKeyword(), param.processCd(),
                scope.companyWide(), scope.accessibleSiteCds(), param.gvCmpnyCd());

        List<AdminContentOptionResult> list = appAdminTbmMapper.selectContentOptions(query);
        return AdminContentOptionResponse.builder()
                .contentList(list != null ? list : Collections.emptyList())
                .build();
    }

    @Override
    public AdminRiskOptionResponse selectRiskOptions(AdminOptionParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        AdminOptionQuery query = new AdminOptionQuery(
                param.siteCd(), param.searchKeyword(), param.processCd(),
                scope.companyWide(), scope.accessibleSiteCds(), param.gvCmpnyCd());

        List<AdminRiskOptionResult> list = appAdminTbmMapper.selectRiskOptions(query);

        List<AdminRiskOptionResponse.RiskOptionItem> items = new ArrayList<>();
        if (list != null) {
            for (AdminRiskOptionResult r : list) {
                items.add(AdminRiskOptionResponse.RiskOptionItem.builder()
                        .siteCd(r.siteCd())
                        .processCd(r.processCd())
                        .processNm(r.processNm())
                        .riskTypeCd(r.riskTypeCd())
                        .riskTypeNm(r.riskTypeNm())
                        .hazardCd(r.hazardCd())
                        .hazardNm(r.hazardNm())
                        .assessmentCd(r.assessmentCd())
                        .assessmentStatus(r.assessmentStatus())
                        .assessmentStatusNm(r.assessmentStatusNm())
                        .displayName(buildRiskDisplayName(r.processNm(), r.riskTypeNm(), r.hazardNm()))
                        .build());
            }
        }

        return AdminRiskOptionResponse.builder()
                .riskList(items)
                .build();
    }

    @Override
    public AdminSiteOptionResponse selectSiteOptions(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        // 진입 권한 게이트(master/safe/nodeAdmin). 접근가능 사업장은 access-context 동일 소스.
        resolveScope(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), tokenInfo.gv_siteCd(), tokenInfo.gv_authCd());

        List<AccessibleSiteResult> sites = appAdminAccessMapper.selectAccessibleSites(
                AdminAccessQuery.ofCompany(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd()));

        return AdminSiteOptionResponse.builder()
                .sites(sites != null ? sites : Collections.emptyList())
                .build();
    }

    // ============================ R3 T1 교육 시작 ============================

    @Override
    @Transactional
    public AdminLiveTransitionResponse startSession(AdminLiveTransitionParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // T1: 개설자만

        int updated = appAdminTbmMapper.startSession(
                AdminSessionTransitionCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 가드(STATUS_CD='OPENED') 0건 = 이미 진행/종료 등 전이 충돌.
            log.warn("TBM 교육 시작 상태 전이 충돌 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_013);
        }

        // 동기화 상태 PLAYING UPSERT(실시간 동기화 도입 대비 초기값). 브로드캐스트는 R3 범위 외.
        appAdminTbmMapper.upsertSessionStatePlaying(
                AdminSessionStateCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));

        // 시작 시각은 DB NOW() 기록값을 재조회해 응답(포맷 일관성).
        AdminSessionResult session = appAdminTbmMapper.selectSessionDetail(
                AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd()));

        log.info("앱 관리자 TBM 교육 시작 완료 - sessionCd={}, manager={}", param.sessionCd(), param.gvUserCd());

        return AdminLiveTransitionResponse.builder()
                .sessionCd(param.sessionCd())
                .statusCd("IN_PROGRESS")
                .startedAt(session != null ? session.startedAt() : null)
                .build();
    }

    // ============================ R3 T1 교육 종료 + T2 자동이수 ============================

    @Override
    @Transactional
    public AdminLiveTransitionResponse endSession(AdminLiveTransitionParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // T1: 개설자만

        AdminSessionTransitionCommand command =
                AdminSessionTransitionCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd());

        int updated = appAdminTbmMapper.endSession(command);
        if (updated == 0) {
            // 가드(STATUS_CD='IN_PROGRESS') 0건 = 진행 중이 아님(전이 충돌).
            log.warn("TBM 교육 종료 상태 전이 충돌 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_014);
        }

        // T2: 미종료(EXIT_AT IS NULL) 출결 일괄 자동이수(EXIT_TYPE_CD 는 NULL 유지 — SYS052 종료자동 코드 없음).
        int autoCompletedCount = appAdminTbmMapper.autoCompleteOnEnd(command);

        AdminSessionResult session = appAdminTbmMapper.selectSessionDetail(
                AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd()));

        log.info("앱 관리자 TBM 교육 종료 완료 - sessionCd={}, autoCompletedCount={}",
                param.sessionCd(), autoCompletedCount);

        return AdminLiveTransitionResponse.builder()
                .sessionCd(param.sessionCd())
                .statusCd("COMPLETED")
                .endedAt(session != null ? session.endedAt() : null)
                .autoCompletedCount(autoCompletedCount)
                .build();
    }

    // ============================ R3 출결 리스트(LIVE/COMPLETED) ============================

    @Override
    public AdminAttendeeListResponse selectAttendees(AdminAttendeeListParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 세션 소유/스코프 재검증(IDOR). 노드관리자는 사업장 + 개설자 노드 모두 통과해야 한다.
        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        boolean liveOnly = "LIVE".equals(param.phase());
        List<AdminAttendeeResult> list = appAdminTbmMapper.selectSessionAttendeesAdmin(
                AdminAttendeeListQuery.of(param.sessionCd(), param.gvCmpnyCd(), liveOnly));

        log.info("앱 관리자 TBM 출결 리스트 조회 완료 - sessionCd={}, phase={}, count={}",
                param.sessionCd(), param.phase(), list != null ? list.size() : 0);

        return AdminAttendeeListResponse.builder()
                .attendees(list != null ? list : Collections.emptyList())
                .totalCount(list != null ? list.size() : 0)
                .build();
    }

    // ============================ R3 T3 강제 퇴실 ============================

    @Override
    @Transactional
    public AdminForceExitResponse forceExitAttendee(AdminForceExitParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.attendanceCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        // 진행 중 세션에서만 강제 퇴실 허용.
        if (!"IN_PROGRESS".equals(guard.statusCd())) {
            log.warn("TBM 강제 퇴실 불가 세션 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_015);
        }

        // 사유는 선택값. 빈문자는 NULL 로 정규화(EXIT_FORCED_REASON nullable).
        String reason = StringUtils.hasText(param.reason()) ? param.reason().trim() : null;

        int updated = appAdminTbmMapper.forceExitAttendee(AdminForceExitCommand.of(
                param.sessionCd(), param.attendanceCd(), reason, param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 멱등 가드(EXIT_AT IS NULL) 0건 = 이미 퇴실 또는 대상 없음(타 세션/회사 attendanceCd 포함).
            log.warn("TBM 강제 퇴실 대상 없음/이미 퇴실 - sessionCd={}, attendanceCd={}",
                    param.sessionCd(), param.attendanceCd());
            throw new ApiException(TbmErrorCode.TBM_409_016);
        }

        log.info("앱 관리자 TBM 강제 퇴실 완료 - sessionCd={}, attendanceCd={}, byManager={}",
                param.sessionCd(), param.attendanceCd(), param.gvUserCd());

        return AdminForceExitResponse.builder()
                .attendanceCd(param.attendanceCd())
                .exitAt(nowMinute())
                .completionStatusCd("NOT_COMPLETED")
                .build();
    }

    // ============================ R3 T4 개별 이수처리 ============================

    @Override
    @Transactional
    public AdminCompletionResponse updateAttendeeCompletion(AdminCompletionParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.attendanceCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 이수상태 코드 검증.
        boolean completed = "COMPLETED".equals(param.completionStatusCd());
        boolean notCompleted = "NOT_COMPLETED".equals(param.completionStatusCd());
        if (!completed && !notCompleted) {
            throw new ApiException(TbmErrorCode.TBM_400_020);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        // T4: GPS 검증 세션(AUTO/MANUAL)에서만 개별 이수처리 허용. DISABLED 거부(서버 검증).
        if ("DISABLED".equals(guard.gpsVerifyTypeCd())) {
            log.warn("TBM 개별 이수처리 불가(GPS 비검증 세션) - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_403_013);
        }

        // 종료된 세션에서만 보정 가능.
        if (!"COMPLETED".equals(guard.statusCd())) {
            log.warn("TBM 개별 이수처리 불가 세션 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_017);
        }

        // 미이수 시 사유 10자 이상 서버 검증. 이수 시 사유 NULL.
        String notCompletedReason = null;
        if (notCompleted) {
            String reason = param.reason() != null ? param.reason().trim() : "";
            if (reason.length() < NOT_COMPLETED_REASON_MIN) {
                throw new ApiException(TbmErrorCode.TBM_400_021);
            }
            notCompletedReason = reason;
        }

        int updated = appAdminTbmMapper.updateAttendeeCompletion(AdminCompletionCommand.of(
                param.sessionCd(), param.attendanceCd(), param.completionStatusCd(),
                notCompletedReason, param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 대상 출결 없음(타 세션/회사 attendanceCd 포함).
            log.warn("TBM 개별 이수처리 대상 없음 - sessionCd={}, attendanceCd={}",
                    param.sessionCd(), param.attendanceCd());
            throw new ApiException(TbmErrorCode.TBM_404_020);
        }

        log.info("앱 관리자 TBM 개별 이수처리 완료 - sessionCd={}, attendanceCd={}, status={}",
                param.sessionCd(), param.attendanceCd(), param.completionStatusCd());

        return AdminCompletionResponse.builder()
                .attendanceCd(param.attendanceCd())
                .completionStatusCd(param.completionStatusCd())
                .build();
    }

    // ============================ R3 진행화면 슬라이드용 자료 항목 ============================

    @Override
    public AdminSessionContentsResponse selectSessionContents(AdminSessionDetailParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        AdminSessionDetailQuery query = AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd());

        List<AdminSessionContentResult> mtrls = appAdminTbmMapper.selectSessionContents(query);
        List<AdminContentItemResult> items = appAdminTbmMapper.selectSessionContentItems(query);

        // 묶음코드별 항목 그룹핑(조회 순서=DISPLAY_ORDER, SORT_IDX 유지).
        Map<String, List<AdminSessionContentsResponse.Item>> itemsByMtrl = new LinkedHashMap<>();
        if (items != null) {
            for (AdminContentItemResult it : items) {
                itemsByMtrl.computeIfAbsent(it.mtrlCd(), k -> new ArrayList<>())
                        .add(AdminSessionContentsResponse.Item.builder()
                                .mtrlItemCd(it.mtrlItemCd())
                                .type(it.itemType())
                                .fileMgmtCd(it.fileMgmtCd())
                                .url(it.url())
                                .itemDesc(it.itemDesc())
                                .sortIdx(it.sortIdx())
                                // 서명 URL 전환: 파일형 항목은 서명 절대 URL(previewUrl) 발급(파일 없으면 NULL).
                                .previewUrl(signPreview(it.filePath(), it.fileMgmtCd(), it.fileExt(), param.gvCmpnyCd()))
                                .build());
            }
        }

        List<AdminSessionContentsResponse.Material> materials = new ArrayList<>();
        if (mtrls != null) {
            for (AdminSessionContentResult m : mtrls) {
                materials.add(AdminSessionContentsResponse.Material.builder()
                        .mtrlCd(m.mtrlCd())
                        .title(m.title())
                        .overrideDesc(m.overrideDesc())
                        .displayOrder(m.displayOrder())
                        .items(itemsByMtrl.getOrDefault(m.mtrlCd(), new ArrayList<>()))
                        .build());
            }
        }

        log.info("앱 관리자 TBM 진행화면 자료 항목 조회 완료 - sessionCd={}, materialCount={}",
                param.sessionCd(), materials.size());

        return AdminSessionContentsResponse.builder()
                .materials(materials)
                .build();
    }

    // ============================ R5 교육자료 리스트 ============================

    @Override
    public AdminEduMaterialListResponse selectEduMaterials(AdminEduMaterialListParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        int offset = (param.page() - 1) * param.pageSize();
        AdminEduMaterialListQuery query = new AdminEduMaterialListQuery(
                param.mtrlType(),
                param.title(),
                param.useYn(),
                scope.companyWide(),
                scope.accessibleSiteCds(),
                offset,
                param.pageSize(),
                param.gvCmpnyCd());

        List<AdminEduMaterialListResult> list = appAdminTbmMapper.selectAdminEduMaterials(query);
        int totalCount = appAdminTbmMapper.selectAdminEduMaterialsCount(query);

        log.info("앱 관리자 TBM 교육자료 리스트 조회 완료 - cmpnyCd={}, companyWide={}, count={}, totalCount={}",
                param.gvCmpnyCd(), scope.companyWide(), list != null ? list.size() : 0, totalCount);

        return AdminEduMaterialListResponse.builder()
                .materials(list != null ? list : Collections.emptyList())
                .totalCount(totalCount)
                .page(param.page())
                .pageSize(param.pageSize())
                .build();
    }

    // ============================ R5 교육자료 상세 ============================

    @Override
    public AdminEduMaterialDetailResponse selectEduMaterialDetail(AdminEduMaterialDetailParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.mtrlCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminEduMaterialDetailQuery query = AdminEduMaterialDetailQuery.of(param.mtrlCd(), param.gvCmpnyCd());

        AdminEduMaterialResult material = appAdminTbmMapper.selectAdminEduMaterial(query);
        if (material == null) {
            throw new ApiException(TbmErrorCode.TBM_404_040);
        }

        // 스코프 격리(IDOR): 비전사 권한은 회사공통(SITE_CD NULL) 또는 접근가능 사업장만 열람 가능.
        verifyMaterialScope(scope, material.siteCd());

        List<AdminEduMaterialItemResult> items = appAdminTbmMapper.selectAdminEduMaterialItems(query);

        // 서명 URL 전환: 원시 결과(filePath/fileExt) → 응답(previewUrl=서명 절대 URL) 매핑.
        List<AdminEduMaterialItemResponse> itemResponses = new ArrayList<>();
        if (items != null) {
            for (AdminEduMaterialItemResult it : items) {
                itemResponses.add(AdminEduMaterialItemResponse.builder()
                        .mtrlItemCd(it.mtrlItemCd())
                        .mtrlItemType(it.mtrlItemType())
                        .mtrlDesc(it.mtrlDesc())
                        .fileMgmtCd(it.fileMgmtCd())
                        .thumbFileMgmtCd(it.thumbFileMgmtCd())
                        .durationSec(it.durationSec())
                        .url(it.url())
                        .sortIdx(it.sortIdx())
                        .previewUrl(signPreview(it.filePath(), it.fileMgmtCd(), it.fileExt(), param.gvCmpnyCd()))
                        .build());
            }
        }

        log.info("앱 관리자 TBM 교육자료 상세 조회 완료 - mtrlCd={}, itemCount={}",
                param.mtrlCd(), itemResponses.size());

        return AdminEduMaterialDetailResponse.builder()
                .material(material)
                .items(itemResponses)
                .build();
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

    // ============================ R5 교육자료 등록 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminEduMaterialSaveResponse saveEduMaterial(AdminEduMaterialSaveParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        // 권한 경계 + 입력 검증(공통).
        validateMaterialSave(param);
        assertMaterialWriteScope(scope, param);

        String mtrlCd = appAdminTbmMapper.selectMtrlCd(param.gvCmpnyCd());
        if (!StringUtils.hasText(mtrlCd)) {
            log.error("TBM 교육자료 코드 채번 실패 - cmpnyCd={}", param.gvCmpnyCd());
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }

        String useYn = "N".equals(param.useYn()) ? "N" : "Y";
        // 회사공통(siteCd 빈값)은 SITE_CD NULL 로 저장.
        String siteCd = StringUtils.hasText(param.siteCd()) ? param.siteCd() : null;

        appAdminTbmMapper.insertAdminEduMaterial(AdminEduMaterialCommand.of(
                mtrlCd, siteCd, param.title().trim(), param.contents(), param.mtrlType(),
                useYn, param.gvCmpnyCd(), param.gvUserCd()));

        // 신규 등록: 보존 가능한 기존 fileMgmtCd 가 존재하지 않으므로 허용 집합은 비움(보존값 전달 시 거부).
        insertMaterialItems(param, mtrlCd, Collections.emptySet());

        log.info("앱 관리자 TBM 교육자료 등록 완료 - mtrlCd={}, common={}, itemCount={}",
                mtrlCd, siteCd == null, param.items().size());

        return AdminEduMaterialSaveResponse.builder().mtrlCd(mtrlCd).build();
    }

    // ============================ R5 교육자료 수정 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminEduMaterialSaveResponse updateEduMaterial(AdminEduMaterialSaveParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.mtrlCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        validateMaterialSave(param);

        // 대상 자료 현재 SITE_CD 로 스코프 재검증(IDOR). 회사공통이면 companyWide 필요.
        AdminEduMaterialResult target = appAdminTbmMapper.selectAdminEduMaterial(
                AdminEduMaterialDetailQuery.of(param.mtrlCd(), param.gvCmpnyCd()));
        if (target == null) {
            throw new ApiException(TbmErrorCode.TBM_404_040);
        }
        assertMaterialWriteScopeForExisting(scope, target.siteCd());

        // 수정 시 "기존 파일 유지" 경로의 fileMgmtCd 소유 검증(IDOR 방어).
        // 동일 회사 + 해당 mtrlCd 의 기존 항목에 속한 fileMgmtCd 집합만 보존 허용.
        java.util.Set<String> ownedFileMgmtCds = loadOwnedFileMgmtCds(param.mtrlCd(), param.gvCmpnyCd());

        String useYn = "N".equals(param.useYn()) ? "N" : "Y";
        // 수정 시 SITE_CD(스코프)는 변경하지 않는다(web Tbm01 패리티). siteCd=null 전달 → 매퍼 IFNULL 보존.
        int updated = appAdminTbmMapper.updateAdminEduMaterial(AdminEduMaterialCommand.of(
                param.mtrlCd(), null, param.title().trim(), param.contents(), param.mtrlType(),
                useYn, param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 회사 가드 0건 = 대상 없음/타 회사(IDOR).
            log.warn("TBM 교육자료 수정 대상 없음 - mtrlCd={}", param.mtrlCd());
            throw new ApiException(TbmErrorCode.TBM_404_040);
        }

        // 항목 재구성: 기존 항목 일괄 소프트삭제 후 신규 항목 INSERT.
        appAdminTbmMapper.softDeleteAdminEduMaterialItems(
                param.gvCmpnyCd(), param.mtrlCd(), param.gvUserCd());
        insertMaterialItems(param, param.mtrlCd(), ownedFileMgmtCds);

        log.info("앱 관리자 TBM 교육자료 수정 완료 - mtrlCd={}, itemCount={}",
                param.mtrlCd(), param.items().size());

        return AdminEduMaterialSaveResponse.builder().mtrlCd(param.mtrlCd()).build();
    }

    // ============================ R5 교육자료 삭제(소프트) ============================

    @Override
    @Transactional
    public void deleteEduMaterial(AdminEduMaterialDetailParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.mtrlCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminEduMaterialResult target = appAdminTbmMapper.selectAdminEduMaterial(
                AdminEduMaterialDetailQuery.of(param.mtrlCd(), param.gvCmpnyCd()));
        if (target == null) {
            throw new ApiException(TbmErrorCode.TBM_404_040);
        }
        assertMaterialWriteScopeForExisting(scope, target.siteCd());

        // 소프트삭제(USE_YN='N'): 세션-콘텐츠 매핑/이력 참조 보존(하드 DELETE 안 함).
        int updated = appAdminTbmMapper.softDeleteAdminEduMaterial(
                param.gvCmpnyCd(), param.mtrlCd(), param.gvUserCd());
        if (updated == 0) {
            throw new ApiException(TbmErrorCode.TBM_404_040);
        }
        appAdminTbmMapper.softDeleteAdminEduMaterialItems(
                param.gvCmpnyCd(), param.mtrlCd(), param.gvUserCd());

        log.info("앱 관리자 TBM 교육자료 삭제(소프트) 완료 - mtrlCd={}", param.mtrlCd());
    }

    // ============================ R5 자료 타입(COM003) 옵션 ============================

    @Override
    public AdminMaterialTypeOptionResponse selectMaterialTypeOptions(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        // 진입 권한 게이트(master/safe/nodeAdmin).
        resolveScope(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), tokenInfo.gv_siteCd(), tokenInfo.gv_authCd());

        List<AdminMaterialTypeOptionResult> options =
                appAdminTbmMapper.selectMaterialTypeOptions(tokenInfo.gv_cmpnyCd());

        return AdminMaterialTypeOptionResponse.builder()
                .options(options != null ? options : Collections.emptyList())
                .build();
    }

    // ============================ R6 이력 리스트 + 통계 ============================

    @Override
    public AdminHistoryListResponse selectHistory(AdminHistoryListParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        int offset = (param.page() - 1) * param.pageSize();
        AdminHistoryListQuery query = new AdminHistoryListQuery(
                param.statusCd(),
                param.startDate(),
                param.endDate(),
                param.keyword(),
                scope.companyWide(),
                scope.accessibleSiteCds(),
                scope.scopedNodeCds(),
                offset,
                param.pageSize(),
                param.gvCmpnyCd());

        List<AdminHistoryListResult> list = appAdminTbmMapper.selectAdminHistory(query);
        int totalCount = appAdminTbmMapper.selectAdminHistoryCount(query);
        AdminHistoryStatResult stat = appAdminTbmMapper.selectAdminHistoryStat(query);

        AdminHistoryListResponse.StatSummary statSummary = buildHistoryStat(stat);

        log.info("앱 관리자 TBM 이력 리스트 조회 완료 - cmpnyCd={}, companyWide={}, count={}, totalCount={}",
                param.gvCmpnyCd(), scope.companyWide(), list != null ? list.size() : 0, totalCount);

        return AdminHistoryListResponse.builder()
                .historyList(list != null ? list : Collections.emptyList())
                .totalCount(totalCount)
                .page(param.page())
                .pageSize(param.pageSize())
                .stat(statSummary)
                .build();
    }

    // ============================ 내부 헬퍼 ============================

    private void insertContents(List<AdminSessionContentModel> contents, String sessionCd,
            String gvCmpnyCd, String gvUserCd) {
        if (contents == null) {
            return;
        }
        int order = 0;
        for (AdminSessionContentModel model : contents) {
            if (!StringUtils.hasText(model.getMtrlCd())) {
                continue;
            }
            appAdminTbmMapper.insertSessionContent(
                    AdminSessionContentCommand.from(model, sessionCd, order, gvCmpnyCd, gvUserCd));
            order++;
        }
    }

    private void insertRisks(List<AdminSessionRiskModel> risks, String sessionCd,
            String gvCmpnyCd, String gvUserCd) {
        if (risks == null) {
            return;
        }
        int order = 0;
        for (AdminSessionRiskModel model : risks) {
            if (!StringUtils.hasText(model.getProcessCd()) || !StringUtils.hasText(model.getAssessmentCd())) {
                continue;
            }
            appAdminTbmMapper.insertSessionRisk(
                    AdminSessionRiskCommand.from(model, sessionCd, order, gvCmpnyCd, gvUserCd));
            order++;
        }
    }

    private AdminSessionGuardResult loadGuard(String gvCmpnyCd, String sessionCd) {
        AdminSessionGuardResult guard = appAdminTbmMapper.selectSessionGuard(
                AdminSessionDetailQuery.of(sessionCd, gvCmpnyCd));
        if (guard == null) {
            throw new ApiException(TbmErrorCode.TBM_404_010);
        }
        return guard;
    }

    /**
     * 스코프 격리(IDOR): master/safe(companyWide)는 통과. 노드관리자는 (1)대상 사업장이 접근가능 사업장이고
     * (2)개설자(MANAGER_USER_CD)의 NODE_CD 가 대상 사업장 기준 자기노드+자손(scopedNodeCds)에 속할 때만 통과(plan §6·T7).
     * 개설자 노드(managerNodeCd)가 없으면(노드 미배정) 비전사 권한은 차단한다.
     */
    private void verifyScope(ScopeContext scope, String targetSiteCd, String managerNodeCd,
            String cmpnyCd, String userCd) {
        if (scope.companyWide()) {
            return;
        }
        // 사업장 축: 대상 사업장이 접근가능 사업장 집합에 포함되어야 한다.
        if (targetSiteCd == null || !scope.accessibleSiteCds().contains(targetSiteCd)) {
            log.warn("TBM 세션 스코프 위반(사업장) - userCd={}, targetSite={}", userCd, targetSiteCd);
            throw new ApiException(TbmErrorCode.TBM_403_011);
        }
        // 노드 축: 개설자 NODE_CD ∈ 대상 사업장 기준 자기노드+자손.
        if (!StringUtils.hasText(managerNodeCd)) {
            log.warn("TBM 세션 스코프 위반(개설자 노드 미배정) - userCd={}, targetSite={}", userCd, targetSiteCd);
            throw new ApiException(TbmErrorCode.TBM_403_011);
        }
        List<String> targetScopedNodes = adminScopeMapper.selectScopedNodeCds(
                ScopedNodeQuery.of(cmpnyCd, targetSiteCd, userCd));
        if (targetScopedNodes == null || !targetScopedNodes.contains(managerNodeCd)) {
            log.warn("TBM 세션 스코프 위반(노드) - userCd={}, managerNodeCd={}, targetSite={}",
                    userCd, managerNodeCd, targetSiteCd);
            throw new ApiException(TbmErrorCode.TBM_403_011);
        }
    }

    /**
     * R3 T1: 교육 시작/종료는 개설자(MANAGER_USER_CD == 토큰 userCd)만 허용한다.
     * 동일 스코프 관리자(verifyScope 통과)라도 개설자가 아니면 차단한다.
     */
    private void verifyManager(AdminSessionGuardResult guard, String userCd) {
        if (!StringUtils.hasText(guard.managerUserCd()) || !guard.managerUserCd().equals(userCd)) {
            log.warn("TBM 라이브 제어 개설자 불일치 - sessionCd={}, manager={}, requester={}",
                    guard.sessionCd(), guard.managerUserCd(), userCd);
            throw new ApiException(TbmErrorCode.TBM_403_012);
        }
    }

    /** DB NOW() 와 동일 분 단위 포맷('yyyy-MM-dd HH:mm'). 강제퇴실 응답 exitAt 표기용. */
    private String nowMinute() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private boolean isPwdVisible(String statusCd, String authCd, boolean companyWide) {
        boolean openStatus = "OPENED".equals(statusCd) || "IN_PROGRESS".equals(statusCd);
        boolean isManager = companyWide || AuthRoleUtils.canManageCommon(authCd) || AuthRoleUtils.isManager(authCd);
        return openStatus && isManager;
    }

    private void validateContentBody(String contentBody) {
        // T5: plain text. 빈 입력 거부를 위해 공백 제거 후 길이만 검사(HTML strip 동일 결과).
        String text = contentBody == null ? "" : contentBody.replaceAll("\\s+", "").trim();
        if (text.length() < CONTENT_TEXT_MIN) {
            log.warn("TBM 교육 내용 텍스트 부족 - len={}", text.length());
            throw new ApiException(TbmErrorCode.TBM_400_010);
        }
    }

    private void validateGps(String gpsVerifyTypeCd, String lat, String lon,
            Integer radiusM, String manualConfirmYn) {
        String type = StringUtils.hasText(gpsVerifyTypeCd) ? gpsVerifyTypeCd : "AUTO";

        if (!"AUTO".equals(type) && !"MANUAL".equals(type) && !"DISABLED".equals(type)) {
            throw new ApiException(TbmErrorCode.TBM_400_012);
        }

        if ("AUTO".equals(type)) {
            if (!StringUtils.hasText(lat) || !StringUtils.hasText(lon)) {
                log.warn("TBM GPS AUTO 좌표 누락");
                throw new ApiException(TbmErrorCode.TBM_400_012);
            }
        } else if ("MANUAL".equals(type)) {
            if (!"Y".equals(manualConfirmYn)) {
                log.warn("TBM GPS MANUAL 확인 누락");
                throw new ApiException(TbmErrorCode.TBM_400_012);
            }
        }

        if (radiusM != null && (radiusM < GPS_RADIUS_MIN || radiusM > GPS_RADIUS_MAX)) {
            throw new ApiException(TbmErrorCode.TBM_400_013);
        }
    }

    private String buildRiskDisplayName(String processNm, String riskTypeNm, String hazardNm) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(processNm)) {
            parts.add(processNm);
        }
        if (StringUtils.hasText(riskTypeNm)) {
            parts.add(riskTypeNm);
        }
        if (StringUtils.hasText(hazardNm)) {
            parts.add(hazardNm);
        }
        return String.join(" > ", parts);
    }

    // ============================ R5 교육자료 내부 헬퍼 ============================

    /** 자료 등록/수정 공통 입력 검증(권위 서버 검증). */
    private void validateMaterialSave(AdminEduMaterialSaveParam param) {
        String title = param.title() != null ? param.title().trim() : "";
        if (!StringUtils.hasText(title) || title.length() > TITLE_MAX) {
            throw new ApiException(TbmErrorCode.TBM_400_040);
        }
        // 자료 타입(COM003) 유효성 — 회사 코드그룹에 존재하는지 확인.
        if (!StringUtils.hasText(param.mtrlType()) || !isValidMaterialType(param.gvCmpnyCd(), param.mtrlType())) {
            throw new ApiException(TbmErrorCode.TBM_400_041);
        }
        // 항목 1개 이상.
        if (param.items() == null || param.items().isEmpty()) {
            throw new ApiException(TbmErrorCode.TBM_400_042);
        }
        // 항목별: 타입 allow-list + 파일/URL 구성 검증.
        for (var item : param.items()) {
            String type = item.getMtrlItemType();
            if (!isAllowedItemType(type)) {
                throw new ApiException(TbmErrorCode.TBM_400_042);
            }
            if (ITEM_TYPE_URL.equals(type)) {
                if (!StringUtils.hasText(item.getUrl())) {
                    throw new ApiException(TbmErrorCode.TBM_400_042);
                }
                validateExternalUrl(item.getUrl());
            } else {
                // 파일형: 신규 업로드(fileIndex) 또는 기존 유지(fileMgmtCd) 중 하나 필수.
                boolean hasNewFile = item.getFileIndex() != null;
                boolean hasExisting = StringUtils.hasText(item.getFileMgmtCd());
                if (!hasNewFile && !hasExisting) {
                    throw new ApiException(TbmErrorCode.TBM_400_042);
                }
                if (hasNewFile) {
                    int idx = item.getFileIndex();
                    if (idx < 0 || idx >= param.files().size()) {
                        throw new ApiException(TbmErrorCode.TBM_400_042);
                    }
                    validateUploadFile(param.files().get(idx));
                }
            }
        }
    }

    /**
     * 외부링크(항목타입 03) URL 서버 검증(권위).
     * <p>http:// 또는 https:// 스킴만 허용(대소문자 무시). javascript:/data:/file: 등 거부.
     * 길이는 컬럼 정합(≤{@value #URL_MAX_LEN}).
     */
    private void validateExternalUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.length() > URL_MAX_LEN) {
            log.warn("TBM 교육자료 외부링크 URL 길이 초과 - len={}", trimmed.length());
            throw new ApiException(TbmErrorCode.TBM_400_042);
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            log.warn("TBM 교육자료 외부링크 URL 스킴 불허(http/https 외) - url={}", trimmed);
            throw new ApiException(TbmErrorCode.TBM_400_042);
        }
    }

    /**
     * 자료 항목 INSERT(파일형은 fileIndex→저장→FILE_MGMT_CD, 기존 유지는 fileMgmtCd 보존).
     *
     * @param allowedFileMgmtCds 보존(기존 유지) 허용 fileMgmtCd 집합. 클라가 제공한 보존값이
     *        이 집합(동일 회사 + 해당 mtrlCd 의 기존 항목)에 속하지 않으면 거부(IDOR 방어).
     */
    private void insertMaterialItems(AdminEduMaterialSaveParam param, String mtrlCd,
            java.util.Set<String> allowedFileMgmtCds) {
        int sort = 0;
        for (var item : param.items()) {
            String type = item.getMtrlItemType();
            String fileMgmtCd = null;
            String url = null;

            if (ITEM_TYPE_URL.equals(type)) {
                url = item.getUrl();
            } else {
                if (item.getFileIndex() != null) {
                    // 신규 업로드: 저장 → FILE_MGMT_CD 발급(공통 인프라 재사용).
                    fileMgmtCd = saveMaterialFile(param, param.files().get(item.getFileIndex()));
                } else {
                    // 기존 파일 유지: 클라 제공 fileMgmtCd 가 소유 집합에 속하는지 검증.
                    fileMgmtCd = item.getFileMgmtCd();
                    if (StringUtils.hasText(fileMgmtCd) && !allowedFileMgmtCds.contains(fileMgmtCd)) {
                        log.warn("TBM 교육자료 보존 fileMgmtCd 소유 위반(IDOR 차단) - mtrlCd={}, fileMgmtCd={}",
                                mtrlCd, fileMgmtCd);
                        throw new ApiException(TbmErrorCode.TBM_400_042);
                    }
                }
            }

            String mtrlItemCd = appAdminTbmMapper.selectMtrlItemCd(param.gvCmpnyCd());
            if (!StringUtils.hasText(mtrlItemCd)) {
                log.error("TBM 교육자료 항목 코드 채번 실패 - cmpnyCd={}", param.gvCmpnyCd());
                throw new ApiException(CommonErrorCode.COMMON_500_001);
            }

            int sortIdx = item.getSortIdx() != null ? item.getSortIdx() : sort;
            appAdminTbmMapper.insertAdminEduMaterialItem(AdminEduMaterialItemCommand.of(
                    mtrlItemCd, mtrlCd, sortIdx, type, item.getMtrlDesc(), fileMgmtCd, url,
                    param.gvCmpnyCd(), param.gvUserCd()));
            sort++;
        }
    }

    /**
     * 수정 대상 mtrlCd 의 기존 항목에 속한 fileMgmtCd 집합(동일 회사 한정) 조회.
     * <p>selectAdminEduMaterialItems 는 회사(cmpnyCd) 스코프 쿼리이므로 타 회사 코드는 포함되지 않는다.
     * "기존 파일 유지" 보존값 소유 검증의 화이트리스트로 사용한다.
     */
    private java.util.Set<String> loadOwnedFileMgmtCds(String mtrlCd, String cmpnyCd) {
        List<AdminEduMaterialItemResult> existing = appAdminTbmMapper.selectAdminEduMaterialItems(
                AdminEduMaterialDetailQuery.of(mtrlCd, cmpnyCd));
        java.util.Set<String> owned = new java.util.HashSet<>();
        if (existing != null) {
            for (AdminEduMaterialItemResult it : existing) {
                if (StringUtils.hasText(it.fileMgmtCd())) {
                    owned.add(it.fileMgmtCd());
                }
            }
        }
        return owned;
    }

    /** 자료 파일 저장 → FILE_MGMT_CD 발급. web Tbm01 정합(FILE_TYPE='003', 회사 공통 디렉토리). */
    private String saveMaterialFile(AdminEduMaterialSaveParam param, MultipartFile file) {
        String fileMgmtCd = fileMapper.selectFileMgmtCd(
                FileInfoQuery.from(param.gvCmpnyCd(), FILE_TYPE_TBM_MTRL));
        // 자료 파일은 회사 공통으로 저장(web Tbm01 동일 — siteCd 빈값 폴백은 회사코드).
        String safeSiteCd = StringUtils.hasText(param.siteCd()) ? param.siteCd() : param.gvCmpnyCd();
        fileService.fileSave(FileInfoParam.from(
                param.gvCmpnyCd(), param.gvUserCd(), safeSiteCd, FILE_TYPE_TBM_MTRL, fileMgmtCd, file));
        return fileMgmtCd;
    }

    /**
     * 업로드 파일 검증(도메인 한정): 빈 파일/크기 상한/확장자(이미지·동영상·PDF) 화이트리스트
     * + content-type↔확장자 정합 + 매직바이트 시그니처 검증(확장자 위장 1차 차단).
     *
     * <p>확장자 위장(예: payload.png) 방어를 위해 다음을 추가한다.
     * <ul>
     *   <li>(a) 선언 content-type 이 있으면 확장자 그룹(이미지/동영상/PDF)과 모순될 경우 거부.</li>
     *   <li>(b) 이미지/PDF 는 앞부분 매직바이트 시그니처가 확장자와 일치해야 통과.
     *       동영상(mp4/mov/mkv/webm 등)은 컨테이너 시그니처가 다양하여 매직바이트 강제 대신
     *       content-type 정합 + 확장자 화이트리스트로만 처리.</li>
     * </ul>
     * 파일은 앞 {@value #MAGIC_PROBE_BYTES} 바이트만 읽어 판정한다(전체 로딩 금지).
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(TbmErrorCode.TBM_400_042);
        }
        if (file.getSize() > MTRL_FILE_MAX_BYTES) {
            log.info("TBM 교육자료 업로드 파일 크기 초과 - size={}", file.getSize());
            throw new ApiException(TbmErrorCode.TBM_400_043);
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null || !MTRL_ALLOWED_EXTS.contains(ext)) {
            log.warn("TBM 교육자료 업로드 파일 형식 불허 - filename={}", file.getOriginalFilename());
            throw new ApiException(TbmErrorCode.TBM_400_043);
        }

        String group = extGroupOf(ext);   // IMAGE / VIDEO / PDF

        // (a) content-type 정합: 선언된 content-type 이 있으면 확장자 그룹과 모순되면 거부.
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            String ct = contentType.toLowerCase();
            boolean ctOk;
            switch (group) {
                case "IMAGE":
                    ctOk = ct.startsWith("image/");
                    break;
                case "VIDEO":
                    ctOk = ct.startsWith("video/");
                    break;
                case "PDF":
                    ctOk = ct.equals("application/pdf") || ct.equals("application/octet-stream");
                    break;
                default:
                    ctOk = false;
            }
            if (!ctOk) {
                log.warn("TBM 교육자료 업로드 content-type 불일치 - ext={}, contentType={}", ext, contentType);
                throw new ApiException(TbmErrorCode.TBM_400_043);
            }
        }

        // (b) 매직바이트 시그니처(이미지/PDF). 동영상은 컨테이너 다양성으로 강제하지 않음.
        if ("IMAGE".equals(group) || "PDF".equals(group)) {
            byte[] head = readHead(file, MAGIC_PROBE_BYTES);
            if (!matchesMagic(ext, head)) {
                log.warn("TBM 교육자료 업로드 매직바이트 불일치(확장자 위장 의심) - ext={}, filename={}",
                        ext, file.getOriginalFilename());
                throw new ApiException(TbmErrorCode.TBM_400_043);
            }
        }
    }

    /** 확장자 그룹 분류(IMAGE/VIDEO/PDF). 화이트리스트 통과 후 호출. */
    private String extGroupOf(String ext) {
        if ("pdf".equals(ext)) {
            return "PDF";
        }
        if (IMAGE_EXTS.contains(ext)) {
            return "IMAGE";
        }
        return "VIDEO";
    }

    /** 파일 앞 n 바이트만 읽는다(전체 로딩 금지). 실패 시 빈 배열. */
    private byte[] readHead(MultipartFile file, int n) {
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] buf = new byte[n];
            int read = in.readNBytes(buf, 0, n);
            if (read == n) {
                return buf;
            }
            byte[] trimmed = new byte[read];
            System.arraycopy(buf, 0, trimmed, 0, read);
            return trimmed;
        } catch (java.io.IOException e) {
            log.warn("TBM 교육자료 업로드 헤더 읽기 실패 - filename={}", file.getOriginalFilename(), e);
            throw new ApiException(TbmErrorCode.TBM_400_043);
        }
    }

    /**
     * 확장자별 매직바이트 시그니처 일치 여부.
     * <ul>
     *   <li>JPEG(jpg/jpeg): FF D8 FF</li>
     *   <li>PNG: 89 50 4E 47 0D 0A 1A 0A</li>
     *   <li>GIF: 47 49 46 38 (GIF8)</li>
     *   <li>BMP: 42 4D (BM)</li>
     *   <li>WEBP: RIFF(52 49 46 46) .... WEBP(57 45 42 50 @ offset 8)</li>
     *   <li>PDF: 25 50 44 46 (%PDF)</li>
     * </ul>
     * tif/tiff/heic/heif 등 다른 화이트리스트 이미지는 PDF/JPEG/PNG/GIF/BMP/WEBP 와 달리
     * 시그니처를 강제하지 않는다(false positive 회피 — content-type 정합으로만 1차 방어).
     */
    private boolean matchesMagic(String ext, byte[] h) {
        if (h == null) {
            return false;
        }
        switch (ext) {
            case "jpg":
            case "jpeg":
                return startsWith(h, 0xFF, 0xD8, 0xFF);
            case "png":
                return startsWith(h, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif":
                return startsWith(h, 0x47, 0x49, 0x46, 0x38);
            case "bmp":
                return startsWith(h, 0x42, 0x4D);
            case "webp":
                // RIFF....WEBP (앞 4바이트 RIFF, offset 8~11 WEBP)
                return startsWith(h, 0x52, 0x49, 0x46, 0x46)
                        && h.length >= 12
                        && (h[8] & 0xFF) == 0x57 && (h[9] & 0xFF) == 0x45
                        && (h[10] & 0xFF) == 0x42 && (h[11] & 0xFF) == 0x50;
            case "pdf":
                return startsWith(h, 0x25, 0x50, 0x44, 0x46);
            default:
                // 시그니처를 강제하지 않는 이미지 확장자(tif/heic 등)는 통과(content-type 정합으로 1차 방어).
                return true;
        }
    }

    /** head 가 주어진 바이트 시퀀스로 시작하는지 검사. */
    private boolean startsWith(byte[] h, int... sig) {
        if (h.length < sig.length) {
            return false;
        }
        for (int i = 0; i < sig.length; i++) {
            if ((h[i] & 0xFF) != (sig[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /** 원본 파일명에서 소문자 확장자 추출(경로 구분자 제거 — traversal 방어). 없으면 null. */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        String safe = originalFilename.replace("\\", "/");
        safe = safe.substring(safe.lastIndexOf('/') + 1);
        int dot = safe.lastIndexOf('.');
        if (dot < 0 || dot == safe.length() - 1) {
            return null;
        }
        return safe.substring(dot + 1).toLowerCase();
    }

    private boolean isAllowedItemType(String type) {
        return ITEM_TYPE_IMAGE.equals(type)
            || ITEM_TYPE_VIDEO.equals(type)
            || ITEM_TYPE_URL.equals(type)
            || ITEM_TYPE_PDF.equals(type);
    }

    /** COM003(TB_BAIM_VAL_D) 코드 존재 검증. */
    private boolean isValidMaterialType(String cmpnyCd, String mtrlType) {
        List<AdminMaterialTypeOptionResult> options = appAdminTbmMapper.selectMaterialTypeOptions(cmpnyCd);
        if (options == null) {
            return false;
        }
        for (AdminMaterialTypeOptionResult o : options) {
            if (mtrlType.equals(o.code())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 자료 조회 스코프 격리(상세). companyWide=통과. 비전사는 회사공통(SITE_CD NULL) 또는 접근가능 사업장만.
     */
    private void verifyMaterialScope(ScopeContext scope, String targetSiteCd) {
        if (scope.companyWide()) {
            return;
        }
        boolean isCommon = !StringUtils.hasText(targetSiteCd);
        if (isCommon) {
            return;
        }
        if (!scope.accessibleSiteCds().contains(targetSiteCd)) {
            log.warn("TBM 교육자료 스코프 위반(조회) - targetSite={}", targetSiteCd);
            throw new ApiException(TbmErrorCode.TBM_403_001);
        }
    }

    /**
     * 자료 등록 쓰기 스코프(신규). 회사공통(siteCd 빈값) = companyWide(master/safe)만.
     * 사업장전용 = 접근가능 사업장(USE_YN='Y') 서버 검증(노드관리자 포함 가능).
     */
    private void assertMaterialWriteScope(ScopeContext scope, AdminEduMaterialSaveParam param) {
        boolean isCommon = !StringUtils.hasText(param.siteCd());
        if (isCommon) {
            if (!AuthRoleUtils.canManageCommon(param.gvAuthCd())) {
                log.warn("TBM 회사공통 자료 쓰기 권한 없음 - authCd={}", param.gvAuthCd());
                throw new ApiException(TbmErrorCode.TBM_403_040);
            }
            return;
        }
        // 사업장전용: 토큰 접근가능 사업장(USE_YN='Y')인지 서버 검증.
        if (appAdminTbmMapper.existsAccessibleSite(param.gvCmpnyCd(), param.gvUserCd(), param.siteCd()) != 1) {
            log.warn("TBM 사업장 자료 쓰기 권한 없음 - userCd={}, siteCd={}", param.gvUserCd(), param.siteCd());
            throw new ApiException(TbmErrorCode.TBM_403_001);
        }
    }

    /**
     * 자료 수정/삭제 쓰기 스코프(기존 대상의 SITE_CD 기준 재검증, IDOR).
     * 대상이 회사공통이면 companyWide(master/safe)만, 사업장전용이면 접근가능 사업장 보유 필요.
     */
    private void assertMaterialWriteScopeForExisting(ScopeContext scope, String targetSiteCd) {
        boolean isCommon = !StringUtils.hasText(targetSiteCd);
        if (isCommon) {
            if (!scope.companyWide()) {
                log.warn("TBM 회사공통 자료 수정/삭제 권한 없음 - companyWide=false");
                throw new ApiException(TbmErrorCode.TBM_403_040);
            }
            return;
        }
        if (scope.companyWide()) {
            return;
        }
        if (!scope.accessibleSiteCds().contains(targetSiteCd)) {
            log.warn("TBM 사업장 자료 수정/삭제 스코프 위반 - targetSite={}", targetSiteCd);
            throw new ApiException(TbmErrorCode.TBM_403_001);
        }
    }

    // ============================ R6 이력 내부 헬퍼 ============================

    /** 이력 통계 요약 변환(평균 이수율 산출, 빈 데이터 0). web Tbm04 buildStatSummary 동형. */
    private AdminHistoryListResponse.StatSummary buildHistoryStat(AdminHistoryStatResult stat) {
        if (stat == null) {
            return AdminHistoryListResponse.StatSummary.builder()
                    .sessionCount(0)
                    .attendanceCount(0)
                    .completedCount(0)
                    .notCompletedCount(0)
                    .avgCompletionRate(0d)
                    .build();
        }
        double rate = completionRate(stat.completedCount(), stat.attendanceCount());
        return AdminHistoryListResponse.StatSummary.builder()
                .sessionCount(stat.sessionCount())
                .attendanceCount(stat.attendanceCount())
                .completedCount(stat.completedCount())
                .notCompletedCount(stat.notCompletedCount())
                .avgCompletionRate(rate)
                .build();
    }

    /** 이수율(%) = 이수/총참여 * 100. 분모 0 이면 0. 소수 첫째 자리 반올림(web Tbm04 산식 동형). */
    private double completionRate(int completed, int total) {
        if (total <= 0) {
            return 0d;
        }
        double rate = (completed * 100d) / total;
        return Math.round(rate * 10d) / 10d;
    }
}
