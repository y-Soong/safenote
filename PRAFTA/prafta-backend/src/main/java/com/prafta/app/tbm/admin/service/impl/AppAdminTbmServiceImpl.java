package com.prafta.app.tbm.admin.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.tbmshare.result.SessionOwnerResult;
import com.prafta.common.cmm.tbmshare.service.TbmSessionShareService;
import com.prafta.app.admin.access.application.query.AdminAccessQuery;
import com.prafta.app.admin.access.mapper.AppAdminAccessMapper;
import com.prafta.app.admin.access.result.AccessibleSiteResult;
import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.tbm.admin.application.command.AdminCancelEntryCommand;
import com.prafta.app.tbm.admin.application.command.AdminCompletionCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialItemCommand;
import com.prafta.app.tbm.admin.application.command.AdminForceExitCommand;
import com.prafta.app.tbm.admin.application.command.AdminManagerEnterCommand;
import com.prafta.app.tbm.admin.application.command.AdminManagerSignCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCancelCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionContentCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionEndCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionPrepareCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionRiskCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionSinglePwdCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionStateCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionTransitionCommand;
import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;
import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;
import com.prafta.app.tbm.admin.application.param.AdminAttendeeListParam;
import com.prafta.app.tbm.admin.application.param.AdminCompletionParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialListParam;
import com.prafta.app.tbm.admin.application.param.AdminCancelEntryParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminEligibleRegularParam;
import com.prafta.app.tbm.admin.application.param.AdminForceExitParam;
import com.prafta.app.tbm.admin.application.param.AdminHistoryListParam;
import com.prafta.app.tbm.admin.application.param.AdminManagerDirectParam;
import com.prafta.app.tbm.admin.application.param.AdminManagerSignParam;
import com.prafta.app.tbm.admin.application.param.AdminQrScanParam;
import com.prafta.app.tbm.admin.application.param.AdminLiveTransitionParam;
import com.prafta.app.tbm.admin.application.param.AdminOptionParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionCancelParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionListParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPrepareParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPwdParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionUpdateParam;
import com.prafta.app.tbm.admin.application.query.AdminAttendeeListQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialListQuery;
import com.prafta.app.tbm.admin.application.query.AdminEligibleRegularQuery;
import com.prafta.app.tbm.admin.application.query.AdminEntryTargetQuery;
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
import com.prafta.app.tbm.admin.dto.response.AdminEligibleRegularResponse;
import com.prafta.app.tbm.admin.dto.response.AdminForceExitResponse;
import com.prafta.app.tbm.admin.dto.response.AdminHistoryListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminManagerDirectResponse;
import com.prafta.app.tbm.admin.dto.response.AdminQrScanResponse;
import com.prafta.app.tbm.admin.dto.response.AdminLiveTransitionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminManagerSignResponse;
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
import com.prafta.app.tbm.admin.result.AdminCancelEntrySnapshotResult;
import com.prafta.app.tbm.admin.result.AdminContentItemResult;
import com.prafta.app.tbm.admin.result.AdminContentOptionResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialItemResult;
import com.prafta.app.tbm.admin.result.AdminEligibleRegularResult;
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
import com.prafta.common.cmm.push.TbmEventNotiService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.FileUrlSigner;
import com.prafta.common.security.crypto.GpsCoordCrypto;
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
    private final ObjectMapper objectMapper;                   // E11 QR 페이로드(JSON) 파싱
    /** PRAFTA-APP-021-3b(W3): TBM 교육 시작/종료 통보 PUSH 생산자(입실 참석자 대상, afterCommit 격리). */
    private final TbmEventNotiService tbmEventNotiService;
    /** PRAFTA-SUBCON-T5: 연동 회사 지정 공통 검증 지점(입실 범위 게이트/소속 relabel). */
    private final TbmSessionShareService tbmSessionShareService;

    /** GPS좌표-암호화-전환-06: 관리자 좌표 AES-GCM 암복호화(쓰기 암호화 + 상세 fallback 복호화). */
    private final GpsCoordCrypto gpsCoordCrypto;

    private static final int PWD_LENGTH = 6;
    /** prafta-051 §2: 교육준비→교육시작 자동전이 기본 경과시간(분). properties 로 오버라이드 가능(#DF-3). */
    private static final int DEFAULT_PREP_AUTO_START_MINUTES = 15;
    /** 15분 자동 교육시작 경과 임계(분). 기본 15, application.properties 의 tbm.prep.auto-start-minutes 로 조정. */
    @Value("${tbm.prep.auto-start-minutes:15}")
    private int prepAutoStartMinutes = DEFAULT_PREP_AUTO_START_MINUTES;
    private static final int GPS_RADIUS_MIN = 50;
    private static final int GPS_RADIUS_MAX = 1000;
    private static final int CONTENT_TEXT_MIN = 10;
    /** 교육 인정시간(분) 범위(요청서 17.3.1 / 공유계약: 1~60 정수, 웹과 동일 규칙). */
    private static final int EDU_MINUTES_MIN = 1;
    private static final int EDU_MINUTES_MAX = 60;
    private static final int NOT_COMPLETED_REASON_MIN = 10;   // R3 T4 미이수 사유 최소 길이
    private static final int QR_PAYLOAD_MAX_LENGTH = 4096;     // R-D 일용직 QR 페이로드 방어적 길이 상한(정상 ~3KB)

    // ===== R5 교육자료 =====
    private static final int TITLE_MAX = 200;
    /** 자료 항목 타입(SYS018). */
    private static final String ITEM_TYPE_IMAGE = "01";
    private static final String ITEM_TYPE_VIDEO = "02";
    private static final String ITEM_TYPE_URL = "03";
    private static final String ITEM_TYPE_PDF = "04";
    /** TB_FILE_INFO.FILE_TYPE — '003' TBM 교육자료(web Tbm01 정합, 디렉토리 그룹). */
    private static final String FILE_TYPE_TBM_MTRL = "003";

    // ===== tbm04-manager-sign 주관자 서명(참석자 exit 서명 c-003 규칙 동형) =====
    /** TB_FILE_INFO.FILE_TYPE — '003' TBM 서명(출결 서명과 동일 체계). */
    private static final String FILE_TYPE_TBM_SIGN = "003";
    /** 서명 파일 허용 contentType 화이트리스트(PNG/JPEG). */
    private static final String SIGN_CONTENT_TYPE_PNG = "image/png";
    private static final String SIGN_CONTENT_TYPE_JPEG = "image/jpeg";
    /** 서명 파일 크기 상한(5MB). */
    private static final long SIGN_FILE_MAX_BYTES = 5L * 1024 * 1024;
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

        // prafta-051 §2: 진입 시 15분 자동 교육시작 지연평가(멱등). 평가 후 상태를 재조회해 응답에 반영.
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

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

        // prafta-051: 비번 노출 시점 정합 — ENTRY_PWD=OPENED 이상, EXIT_PWD=COMPLETED 이상(관리자 한정).
        boolean entryPwdVisible = isEntryPwdVisible(session.statusCd(), param.gvAuthCd(), scope.companyWide());
        boolean exitPwdVisible = isExitPwdVisible(session.statusCd(), param.gvAuthCd(), scope.companyWide());
        boolean pwdVisible = entryPwdVisible || exitPwdVisible;

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
                .entryPwd(entryPwdVisible ? session.entryPwd() : null)
                .exitPwd(exitPwdVisible ? session.exitPwd() : null)
                .pwdVisible(pwdVisible)
                .managerUserCd(session.managerUserCd())
                .managerUserNm(session.managerUserNm())
                // GPS좌표-암호화-전환-06: fallback 복호화(ENC 우선, NULL 이면 구 평문) — 복호화 평문은
                // scale 7 문자열("37.5010000")이라 기존 decimal→String 응답 포맷과 동일(prefill 라운드트립 호환).
                .managerGpsLat(gpsCoordCrypto.resolveToString(session.managerGpsLatEnc(), session.managerGpsLat()))
                .managerGpsLon(gpsCoordCrypto.resolveToString(session.managerGpsLonEnc(), session.managerGpsLon()))
                .gpsVerifyTypeCd(session.gpsVerifyTypeCd())
                .gpsVerifyRadiusM(session.gpsVerifyRadiusM())
                .eduMinutes(session.eduMinutes())
                .gpsManualConfirmYn(session.gpsManualConfirmYn())
                .openedAt(session.openedAt())
                .prepStartAt(session.prepStartAt())
                .prepAutoStartAt(computePrepAutoStartAt(session.statusCd(), session.prepStartAtEpoch()))
                .startedAt(session.startedAt())
                .endedAt(session.endedAt())
                .cancelledAt(session.cancelledAt())
                .cancelReason(session.cancelReason())
                .insertNm(session.insertNm())
                .insertDate(session.insertDate())
                // tbm04-manager-sign: 이력 상세 사후서명 노출 판단·표시용(파일코드는 미노출).
                .managerSignYn(session.managerSignYn())
                .managerSignedAt(session.managerSignedAt())
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

        // prafta-051 #D-RE1: 개설은 항상 DRAFT 고정. saveMode='OPENED' 직접생성은 제거(거부).
        // 교육준비(OPENED)는 별도 /prepare 전이로만 도달한다(입실비번·GPS·PREP_START_AT 그때 발급).
        if (!"DRAFT".equals(param.saveMode())) {
            log.warn("TBM 세션 저장 모드 부적합(개설=DRAFT만 허용) - saveMode={}", param.saveMode());
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

        // 제목 검증(개설 시점 유일 필수 항목). 교육내용/GPS 는 교육준비 전이에서 최종 강제.
        String title = param.title() != null ? param.title().trim() : "";
        if (!StringUtils.hasText(title) || title.length() > 200) {
            throw new ApiException(TbmErrorCode.TBM_400_011);
        }

        // 교육시간 검증(개설은 NULL 허용, 있으면 1~60). 웹과 동일 규칙.
        validateEduMinutes(param.eduMinutes(), false);

        String sessionCd = appAdminTbmMapper.selectSessionCd(param.gvCmpnyCd());
        if (!StringUtils.hasText(sessionCd)) {
            log.error("TBM 세션코드 채번 실패 - cmpnyCd={}", param.gvCmpnyCd());
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }

        // DRAFT 고정: 비번·GPS좌표·OPENED_AT·PREP_START_AT 미발급(opened=false).
        appAdminTbmMapper.insertSession(
                AdminSessionCommand.forSave(param, sessionCd, "DRAFT", null, null, false));

        insertContents(param.contents(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());
        // 위험성평가 매핑 INSERT — 세션 사업장 정합·실존 검증 포함
        insertRisks(param.risks(), sessionCd, param.siteCd(), param.gvCmpnyCd(), param.gvUserCd());

        // 상태 row 초기 UPSERT(upsertSessionState)는 교육준비(prepare) 전이로 이동 — 개설 시 미수행.

        boolean riskEmpty = param.risks() == null || param.risks().isEmpty();

        log.info("앱 관리자 TBM 세션 개설 완료(DRAFT 고정) - sessionCd={}, riskEmpty={}", sessionCd, riskEmpty);

        return AdminSessionSaveResponse.builder()
                .sessionCd(sessionCd)
                .statusCd("DRAFT")
                .entryPwd(null)
                .exitPwd(null)
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

        // 교육시간 검증(수정은 NULL 허용, 있으면 1~60). 웹과 동일 규칙.
        validateEduMinutes(param.eduMinutes(), false);

        if ("OPENED".equals(guard.statusCd())) {
            validateContentBody(param.contentBody());
            validateGps(param.gpsVerifyTypeCd(), param.managerGpsLat(), param.managerGpsLon(),
                    param.gpsVerifyRadiusM(), param.gpsManualConfirmYn());
        }

        // GPS좌표-암호화-전환-06: 좌표는 검증(validateGps — 암호화 전 원본 문자열 기준) 후 암호화해 저장.
        appAdminTbmMapper.updateSession(AdminSessionCommand.forUpdate(param,
                encryptGpsOrReject(param.managerGpsLat()), encryptGpsOrReject(param.managerGpsLon())));

        appAdminTbmMapper.deleteSessionContents(param.gvCmpnyCd(), param.sessionCd());
        insertContents(param.contents(), param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd());

        appAdminTbmMapper.deleteSessionRisks(param.gvCmpnyCd(), param.sessionCd());
        // 세션 사업장은 수정 불가이므로 guard(DB) 기준으로 정합 검증
        insertRisks(param.risks(), param.sessionCd(), guard.siteCd(), param.gvCmpnyCd(), param.gvUserCd());

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

    /**
     * E6 입실비번 전용 재발급(OPENED 한정). EXIT_PWD 미변경. 기입실자 무영향
     * (비번은 enter 시점에만 검증, 결과는 TB_TBM_ATTENDANCE 에 확정).
     */
    @Override
    @Transactional
    public AdminSessionPwdResponse regenerateEntryPassword(AdminSessionPwdParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // [prafta-051-ra-M1] 비번 재발급도 개설자만(start/end/prepare 동일 기준)

        if (!"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 입실비번 재발급 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_012);
        }

        String entryPwd = NumericPwdGenerator.generate(PWD_LENGTH);
        int affected = appAdminTbmMapper.updateEntryPwd(AdminSessionSinglePwdCommand.of(
                param.sessionCd(), entryPwd, param.gvCmpnyCd(), param.gvUserCd()));
        if (affected == 0) {
            log.warn("TBM 입실비번 재발급 경합/상태부적합 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_409_012);
        }

        log.info("앱 관리자 TBM 입실비번 재발급 완료 - sessionCd={}", param.sessionCd());

        return AdminSessionPwdResponse.builder()
                .sessionCd(param.sessionCd())
                .entryPwd(entryPwd)
                .exitPwd(null)
                .build();
    }

    /**
     * E7 종료비번 전용 재발급(COMPLETED 한정). ENTRY_PWD 미변경. 기종료자 무영향.
     */
    @Override
    @Transactional
    public AdminSessionPwdResponse regenerateExitPassword(AdminSessionPwdParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // [prafta-051-ra-M1] 비번 재발급도 개설자만(start/end/prepare 동일 기준)

        if (!"COMPLETED".equals(guard.statusCd())) {
            log.warn("TBM 종료비번 재발급 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_017);
        }

        String exitPwd = NumericPwdGenerator.generate(PWD_LENGTH);
        int affected = appAdminTbmMapper.updateExitPwd(AdminSessionSinglePwdCommand.of(
                param.sessionCd(), exitPwd, param.gvCmpnyCd(), param.gvUserCd()));
        if (affected == 0) {
            log.warn("TBM 종료비번 재발급 경합/상태부적합 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_409_017);
        }

        log.info("앱 관리자 TBM 종료비번 재발급 완료 - sessionCd={}", param.sessionCd());

        return AdminSessionPwdResponse.builder()
                .sessionCd(param.sessionCd())
                .entryPwd(null)
                .exitPwd(exitPwd)
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
                        .hazardDesc(r.hazardDesc())
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

        // prafta-051 §2/#DF-5: 진입 시 15분 자동전이 멱등 평가. 이미 자동전이됐으면 409 대신 현재상태 멱등 반환.
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

        int updated = appAdminTbmMapper.startSession(
                AdminSessionTransitionCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 가드(STATUS_CD='OPENED') 0건 = 이미 진행(자동/동시전이) 또는 종료 등 전이 충돌.
            AdminSessionResult cur = appAdminTbmMapper.selectSessionDetail(
                    AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd()));
            if (cur != null && "IN_PROGRESS".equals(cur.statusCd())) {
                // 멱등 안내: 자동전이로 이미 교육시작됨 → 현재 상태 반환(#DF-5).
                log.info("TBM 교육 시작 멱등(이미 진행중/자동전이) - sessionCd={}", param.sessionCd());
                appAdminTbmMapper.upsertSessionStatePlaying(
                        AdminSessionStateCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));
                return AdminLiveTransitionResponse.builder()
                        .sessionCd(param.sessionCd())
                        .statusCd("IN_PROGRESS")
                        .startedAt(cur.startedAt())
                        .build();
            }
            log.warn("TBM 교육 시작 상태 전이 충돌 - sessionCd={}, status={}",
                    param.sessionCd(), cur != null ? cur.statusCd() : guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_050);
        }

        // 동기화 상태 PLAYING UPSERT(실시간 동기화 도입 대비 초기값). 브로드캐스트는 R3 범위 외.
        appAdminTbmMapper.upsertSessionStatePlaying(
                AdminSessionStateCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));

        // 시작 시각은 DB NOW() 기록값을 재조회해 응답(포맷 일관성).
        AdminSessionResult session = appAdminTbmMapper.selectSessionDetail(
                AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd()));

        log.info("앱 관리자 TBM 교육 시작 완료 - sessionCd={}, manager={}", param.sessionCd(), param.gvUserCd());

        // PRAFTA-APP-021-3b(W3): 교육 시작 시 입실 참석자에게 PUSH 적재(afterCommit 격리, 전이 영향 없음).
        try {
            tbmEventNotiService.notifyTbmStarted(
                    param.gvCmpnyCd(), guard.siteCd(), param.sessionCd(), param.gvUserCd());
        } catch (Exception e) {
            log.error("TBM 교육 시작 통보 PUSH 적재 hook 실패(전이 영향 없음). sessionCd={}", param.sessionCd(), e);
        }

        return AdminLiveTransitionResponse.builder()
                .sessionCd(param.sessionCd())
                .statusCd("IN_PROGRESS")
                .startedAt(session != null ? session.startedAt() : null)
                .build();
    }

    // ============================ prafta-051 R-A 교육준비/연장 ============================

    /**
     * E2 교육준비(OPENED) 전이. DRAFT→OPENED 전이 시 입실비번 발급 + 관리자 GPS 중심좌표 +
     * PREP_START_AT(NOW) 를 확정한다(15분 자동 교육시작 타이머 시작). OPENED_AT 은 감사 보존(IFNULL).
     * 종료비번(EXIT_PWD)은 발급하지 않는다(교육종료 소관).
     *
     * <p>권한=개설자(verifyManager). 가드 WHERE STATUS_CD='DRAFT'(0건=이미 전이/충돌 409).
     * GPS_VERIFY_TYPE_CD='AUTO' 세션이면 관리자 현재좌표(lat/lon) 필수.
     */
    @Override
    @Transactional
    public AdminLiveTransitionResponse prepareSession(AdminSessionPrepareParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // 개설자만(start/end 동일 기준)

        // 전이 가능 시점: DRAFT 만.
        if (!"DRAFT".equals(guard.statusCd())) {
            log.warn("TBM 교육준비 전이 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_013);
        }

        // 교육시간 필수 검증(DB 의 DRAFT 값 기준, 미입력이면 TBM_400_015). 웹과 동일 규칙.
        validateEduMinutes(guard.eduMinutes(), true);

        // GPS 검증세션(AUTO)이면 관리자 현재좌표 필수(개설 시 저장된 GPS_VERIFY_TYPE_CD 기준).
        if ("AUTO".equals(guard.gpsVerifyTypeCd())
                && (!StringUtils.hasText(param.managerGpsLat()) || !StringUtils.hasText(param.managerGpsLon()))) {
            log.warn("TBM 교육준비 GPS(AUTO) 좌표 누락 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_400_012);
        }

        // 입실비번 발급(6자리). 종료비번은 미발급(null 유지).
        String entryPwd = NumericPwdGenerator.generate(PWD_LENGTH);

        // GPS좌표-암호화-전환-06: 좌표는 필수검증(원본 문자열 기준) 후 암호화해 저장.
        int affected = appAdminTbmMapper.prepareSession(AdminSessionPrepareCommand.of(
                param.sessionCd(), entryPwd,
                encryptGpsOrReject(param.managerGpsLat()), encryptGpsOrReject(param.managerGpsLon()),
                param.gvCmpnyCd(), param.gvUserCd()));
        if (affected == 0) {
            // 동시 전이/상태 변경으로 DRAFT 가 아님.
            log.warn("TBM 교육준비 전이 경합/상태부적합 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_409_013);
        }

        // 상태 row 초기 UPSERT(동기화 초기값) — 개설에서 이 시점으로 이동.
        appAdminTbmMapper.upsertSessionState(
                AdminSessionStateCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));

        log.info("앱 관리자 TBM 교육준비 전이 완료(OPENED) - sessionCd={}, manager={}",
                param.sessionCd(), param.gvUserCd());

        return AdminLiveTransitionResponse.builder()
                .sessionCd(param.sessionCd())
                .statusCd("OPENED")
                .build();
    }

    /**
     * E3 교육준비 연장. PREP_START_AT 을 NOW() 로 리셋해 15분 타이머를 재시작한다. 권한=개설자.
     *
     * <p>진입 시 자동전이를 먼저 평가(evaluateAutoStart)하므로, 이미 15분 경과했으면 OPENED 가 아니게 되어
     * 연장 UPDATE 가 0건→409 로 거부된다(가드 WHERE STATUS_CD='OPENED').
     */
    @Override
    @Transactional
    public void extendPrep(AdminLiveTransitionParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());

        // 만료된 교육준비는 자동전이시켜 연장 불가로 일관(멱등).
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

        int affected = appAdminTbmMapper.extendPrep(
                AdminSessionTransitionCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));
        if (affected == 0) {
            log.warn("TBM 교육준비 연장 경합/상태부적합 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_409_015);
        }

        log.info("앱 관리자 TBM 교육준비 연장 완료(PREP_START_AT 리셋) - sessionCd={}", param.sessionCd());
    }

    // ============================ R3 T1 교육 종료 ============================
    // [정합성 수정] "사용자가 직접 완료해야 이수" 정책 확정:
    //   관리자 종료는 세션 상태만 IN_PROGRESS→COMPLETED 로 전이하며, 참석자를 자동 이수 처리하지 않는다.
    //   종료 후에도 사용자는 본인 완료(서명/exit)를 직접 해야 이수(COMPLETED)가 된다.
    //   (기존 autoCompleteOnEnd 강제 자동이수 + EXIT_AT 일괄세팅 부작용 제거)

    @Override
    @Transactional
    public AdminLiveTransitionResponse endSession(AdminManagerSignParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // T1: 개설자만

        // 상태 사전 가드: 진행 중이 아니면 파일 저장 전에 거부(반복 호출 시 고아 물리 파일 적재 방지).
        // 경합 최종 방어는 아래 UPDATE WHERE(STATUS_CD='IN_PROGRESS') 가드가 담당.
        if (!"IN_PROGRESS".equals(guard.statusCd())) {
            throw new ApiException(TbmErrorCode.TBM_409_051);
        }

        // tbm04-manager-sign: 주관자 서명 필수(스킵 불가 — 서버 강제). null/empty/검증 실패 → TBM_400_070.
        MultipartFile signFile = param.signFile();
        if (signFile == null || signFile.isEmpty()) {
            throw new ApiException(TbmErrorCode.TBM_400_070);
        }
        validateManagerSignFile(signFile);

        // 서명 파일 저장(FILE_TYPE 003). siteCd 는 세션 사업장(개설자=자사이므로 타사 혼입 없음).
        // 전이 충돌(0건→409) 시 트랜잭션 롤백으로 TB_FILE_INFO 행은 함께 롤백되지만, 디스크 물리 파일은
        // 남을 수 있다 — 참석자 exit 경로와 동일한 기존 수용 사항(별도 보상 로직 없음).
        String managerSignFileMgmtCd = saveManagerSignFile(
                param.gvCmpnyCd(), param.gvUserCd(), guard.siteCd(), signFile);

        // prafta-051 E5: 종료비번(EXIT_PWD)을 이 전이에서 최초 발급(개설/준비 시 동시발급 제거).
        String exitPwd = NumericPwdGenerator.generate(PWD_LENGTH);

        int updated = appAdminTbmMapper.endSession(AdminSessionEndCommand.of(
                param.sessionCd(), exitPwd, managerSignFileMgmtCd, param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 가드(STATUS_CD='IN_PROGRESS') 0건 = 진행 중이 아님(전이 충돌).
            log.warn("TBM 교육 종료 상태 전이 충돌 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_051);
        }

        // [정합성 수정] 자동이수(autoCompleteOnEnd) 제거.
        //   종료 후에도 미종료(EXIT_AT IS NULL) 입실자는 EXIT_AT/COMPLETION_STATUS_CD 가 그대로 남아
        //   사용자가 직접 exit(서명) 해야 COMPLETED 가 된다. 완료하지 않은 입실자는 미이수로 집계된다.
        AdminSessionResult session = appAdminTbmMapper.selectSessionDetail(
                AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd()));

        log.info("앱 관리자 TBM 교육 종료 완료(자동이수 없음, 사용자 직접 완료 필요) - sessionCd={}",
                param.sessionCd());

        // PRAFTA-APP-021-3b(W3): 교육 종료 시 입실 참석자에게 PUSH 적재(afterCommit 격리, 전이 영향 없음).
        try {
            tbmEventNotiService.notifyTbmCompleted(
                    param.gvCmpnyCd(), guard.siteCd(), param.sessionCd(), param.gvUserCd());
        } catch (Exception e) {
            log.error("TBM 교육 종료 통보 PUSH 적재 hook 실패(전이 영향 없음). sessionCd={}", param.sessionCd(), e);
        }

        return AdminLiveTransitionResponse.builder()
                .sessionCd(param.sessionCd())
                .statusCd("COMPLETED")
                .endedAt(session != null ? session.endedAt() : null)
                // [정합성 수정] 자동이수 폐지 → 항상 0(응답 계약 호환 위해 필드 유지).
                .autoCompletedCount(0)
                .build();
    }

    // ============================ tbm04-manager-sign 사후서명 ============================

    /**
     * 종료(COMPLETED) 세션 사후 주관자 서명 등록.
     * <p>대상 = 서명 필수 편입 이전에 종료되어 MANAGER_SIGN_FILE_MGMT_CD 가 NULL 인 과거 세션.
     * 가드: 개설자 본인(verifyManager) + COMPLETED(TBM_409_071) + 서명 미존재(TBM_409_070 — 재서명 불가).
     */
    @Override
    @Transactional
    public AdminManagerSignResponse signCompletedSession(AdminManagerSignParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());
        verifyManager(guard, param.gvUserCd());   // 개설자 본인만(TBM_403_012 재사용)

        // 상태 가드: 종료된 세션에서만 사후서명 가능.
        if (!"COMPLETED".equals(guard.statusCd())) {
            throw new ApiException(TbmErrorCode.TBM_409_071);
        }
        // 중복 가드: 이미 서명 존재 시 재서명 불가(확정).
        if (StringUtils.hasText(guard.managerSignFileMgmtCd())) {
            throw new ApiException(TbmErrorCode.TBM_409_070);
        }

        // 서명 파일 필수 + 검증(endSession 과 동일 규칙).
        MultipartFile signFile = param.signFile();
        if (signFile == null || signFile.isEmpty()) {
            throw new ApiException(TbmErrorCode.TBM_400_070);
        }
        validateManagerSignFile(signFile);

        String managerSignFileMgmtCd = saveManagerSignFile(
                param.gvCmpnyCd(), param.gvUserCd(), guard.siteCd(), signFile);

        int updated = appAdminTbmMapper.updateManagerSign(AdminManagerSignCommand.of(
                param.sessionCd(), managerSignFileMgmtCd, param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 경합: 가드 선판정 이후 상태/서명이 바뀐 경우 — 재조회로 사유 분기(파일 메타는 롤백).
            AdminSessionGuardResult recheck = loadGuard(param.gvCmpnyCd(), param.sessionCd());
            if (StringUtils.hasText(recheck.managerSignFileMgmtCd())) {
                throw new ApiException(TbmErrorCode.TBM_409_070);
            }
            throw new ApiException(TbmErrorCode.TBM_409_071);
        }

        // 표시용 서명시각은 매퍼 재조회 값(DB NOW() 기록 — Java 시각 기록 금지).
        AdminSessionResult session = appAdminTbmMapper.selectSessionDetail(
                AdminSessionDetailQuery.of(param.sessionCd(), param.gvCmpnyCd()));

        log.info("앱 관리자 TBM 사후 주관자 서명 등록 완료 - sessionCd={}", param.sessionCd());

        return AdminManagerSignResponse.builder()
                .sessionCd(param.sessionCd())
                .managerSignedAt(session != null ? session.managerSignedAt() : null)
                .build();
    }

    /**
     * 주관자 서명 파일 서버측 검증 — AppTbm01ServiceImpl.validateSignatureFile(c-003) 동형 복제.
     * <p>contentType 화이트리스트(PNG/JPEG) + 크기 상한(5MB) + 매직바이트 확인. 위반 시 TBM_400_070.
     * <p>tbm01 의 private 메서드라 공용 추출 대신 동형 복제(추출 시 tbm01 exit 회귀 위험 회피 — plan §3 재량).
     */
    private void validateManagerSignFile(MultipartFile file) {

        // 1) 크기 상한.
        if (file.getSize() > SIGN_FILE_MAX_BYTES) {
            log.info("[tbm-admin] 주관자 서명 파일 크기 초과: size={}", file.getSize());
            throw new ApiException(TbmErrorCode.TBM_400_070);
        }

        // 2) contentType 화이트리스트(PNG/JPEG 만 허용).
        String contentType = file.getContentType();
        boolean allowedType = SIGN_CONTENT_TYPE_PNG.equals(contentType)
                || SIGN_CONTENT_TYPE_JPEG.equals(contentType);
        if (!allowedType) {
            log.info("[tbm-admin] 주관자 서명 파일 타입 불허: contentType={}", contentType);
            throw new ApiException(TbmErrorCode.TBM_400_070);
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
                log.info("[tbm-admin] 주관자 서명 파일 매직바이트 불일치: contentType={}", contentType);
                throw new ApiException(TbmErrorCode.TBM_400_070);
            }
        } catch (java.io.IOException e) {
            log.error("[tbm-admin] 주관자 서명 파일 검증 중 읽기 오류", e);
            throw new ApiException(TbmErrorCode.TBM_400_070);
        }
    }

    /** 주관자 서명 파일 저장 → fileMgmtCd 발급(FILE_TYPE 003 — 참석자 출결 서명과 동일 체계). */
    private String saveManagerSignFile(String cmpnyCd, String userCd, String siteCd, MultipartFile file) {

        String fileMgmtCd = fileMapper.selectFileMgmtCd(
                FileInfoQuery.from(cmpnyCd, FILE_TYPE_TBM_SIGN));

        // siteCd(세션 사업장)가 비어 있으면 파일 경로 안전성 위해 회사코드 폴백(tbm01 선례 동형).
        String safeSiteCd = StringUtils.hasText(siteCd) ? siteCd : cmpnyCd;

        fileService.fileSave(FileInfoParam.from(
                cmpnyCd, userCd, safeSiteCd, FILE_TYPE_TBM_SIGN, fileMgmtCd, file));

        return fileMgmtCd;
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

        // phase=LIVE(진행화면) 와 PREP(교육준비, prafta-051 R-C/E12) 는 입실자만(ENTRY_AT IS NOT NULL).
        // COMPLETED(종료화면) 는 출결 전체. PREP 는 응답의 distanceM 으로 이탈자 판단(정밀좌표 미노출).
        boolean enteredOnly = "LIVE".equals(param.phase()) || "PREP".equals(param.phase());

        // PRAFTA-SUBCON-T5: 출결 스코프를 SESSION_CD 단독으로 넓힌다(타사 참석자 포함).
        //   인가 전제: 위 loadGuard(gvCmpnyCd, sessionCd) + verifyScope 가 "개설사 관리자"만 통과시킨다.
        List<AdminAttendeeResult> list = appAdminTbmMapper.selectSessionAttendeesAdmin(
                AdminAttendeeListQuery.of(param.sessionCd(), param.gvCmpnyCd(), enteredOnly));

        // 소속 relabel(개설사 직하 1차 회사명. 2차 이하 회사명/코드는 응답에 없음).
        Map<String, String> labels = tbmSessionShareService.resolveTier1LabelMap(param.sessionCd());

        List<AdminAttendeeListResponse.AttendeeItem> items = new ArrayList<>();
        if (list != null) {
            for (AdminAttendeeResult r : list) {
                // F7(최소 노출): 타사 참석자는 성명만 노출한다. 부서명(타사 조직구조)은 redaction.
                //   출결 액션은 attendanceCd 로 동작하므로 기능 영향 없음.
                boolean foreign = r.cmpnyCd() != null && !r.cmpnyCd().equals(param.gvCmpnyCd());

                items.add(AdminAttendeeListResponse.AttendeeItem.builder()
                        .attendanceCd(r.attendanceCd())
                        .userNm(r.userNm())
                        .userTypeCd(r.userTypeCd())
                        .deptNm(foreign ? null : r.deptNm())
                        .entryAt(r.entryAt())
                        .exited(r.exited())
                        .exitAt(r.exitAt())
                        .completionStatusCd(r.completionStatusCd())
                        .distanceM(r.distanceM())
                        .affilCmpnyNm(labels.get(r.cmpnyCd()))
                        .build());
            }
        }

        log.info("앱 관리자 TBM 출결 리스트 조회 완료 - sessionCd={}, phase={}, count={}",
                param.sessionCd(), param.phase(), items.size());

        return AdminAttendeeListResponse.builder()
                .attendees(items)
                .totalCount(items.size())
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
            throw new ApiException(TbmErrorCode.TBM_409_052);
        }

        // 사유는 선택값. 빈문자는 NULL 로 정규화(EXIT_FORCED_REASON nullable).
        String reason = StringUtils.hasText(param.reason()) ? param.reason().trim() : null;

        int updated = appAdminTbmMapper.forceExitAttendee(AdminForceExitCommand.of(
                param.sessionCd(), param.attendanceCd(), reason, param.gvCmpnyCd(), param.gvUserCd()));
        if (updated == 0) {
            // 멱등 가드(EXIT_AT IS NULL) 0건 = 이미 퇴실 또는 대상 없음(타 세션/회사 attendanceCd 포함).
            log.warn("TBM 강제 퇴실 대상 없음/이미 퇴실 - sessionCd={}, attendanceCd={}",
                    param.sessionCd(), param.attendanceCd());
            throw new ApiException(TbmErrorCode.TBM_409_053);
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
            throw new ApiException(TbmErrorCode.TBM_409_054);
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

    // ============================ prafta-051 R-B 입실경로(정규직 대리입실) ============================

    /**
     * E9 정규직 대리입실 후보 검색. 세션을 먼저 조회·스코프 검증(cross-site IDOR 차단)한 뒤, 세션 사업장
     * (guard.siteCd) 기준으로 활성 정규직(TB_USER) 후보를 이름/사번으로 검색한다. 클라이언트가 보낸 사업장은
     * 신뢰하지 않는다(web Tbm02.selectEntryCandidates 동형).
     *
     * <p>노드관리자(비 companyWide)는 세션 사업장 기준 자기노드+자손(scopedNodeCds)으로 TB_USER.NODE_CD 를
     * 제한한다(R1~R6 동일 패턴). 이미 입실(DEL_YN='N' 출결)한 사용자는 alreadyEntered=true 로 표시하되 목록에서
     * 제외하지 않는다(프론트가 비활성 처리). PII 최소(userCd/userNm/deptNm).
     */
    @Override
    public AdminEligibleRegularResponse selectEligibleRegulars(AdminEligibleRegularParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 15분 자동 교육시작 지연평가(멱등) 후, 세션 소유/스코프 재검증(IDOR).
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        // D-2: 입실(대리 포함)은 교육준비(OPENED) 상태에서만 의미가 있다. 그 외 상태는 후보 검색 거부.
        if (!"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 대리입실 후보 검색 불가 상태 - sessionCd={}, status={}",
                    param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_040);
        }

        // 노드관리자면 세션 사업장 기준 자기노드+자손 NODE_CD 집합으로 TB_USER 를 제한한다.
        List<String> scopedNodeCds = Collections.emptyList();
        if (!scope.companyWide()) {
            scopedNodeCds = adminScopeMapper.selectScopedNodeCds(
                    ScopedNodeQuery.of(param.gvCmpnyCd(), guard.siteCd(), param.gvUserCd()));
        }

        AdminEligibleRegularQuery query = AdminEligibleRegularQuery.of(
                param.sessionCd(), param.gvCmpnyCd(), guard.siteCd(), param.keyword(),
                scope.companyWide(), scopedNodeCds,
                (param.page() - 1) * param.pageSize(), param.pageSize());

        List<AdminEligibleRegularResult> users = appAdminTbmMapper.selectEligibleRegulars(query);

        log.info("앱 관리자 TBM 정규직 대리입실 후보 검색 완료 - sessionCd={}, count={}",
                param.sessionCd(), users != null ? users.size() : 0);

        return AdminEligibleRegularResponse.builder()
                .users(users != null ? users : Collections.emptyList())
                .page(param.page())
                .pageSize(param.pageSize())
                .build();
    }

    /**
     * E10 정규직 관리자 대리입실(MANAGER_DIRECT). 세션 OPENED(D-2) + 대상이 세션 사업장 소속 활성 정규직인지
     * 서버 재검증(IDOR) 후 TB_TBM_ATTENDANCE 에 입실 INSERT 한다. 비번/GPS 반경검증은 하지 않고(D-4),
     * ENTRY_GPS_LAT/LON 은 세션의 관리자 좌표를 감사용으로 복사한다(매퍼 INSERT...SELECT).
     *
     * <p>UK(CMPNY,SESSION,USER_TYPE,USER) 충돌(이미 입실 또는 동시 입실 경합)은 DuplicateKeyException 으로
     * 멱등 안내(TBM_409_041)한다. 내보내기(DEL_YN='Y') 후 재입실 복구는 R-C(#D-RE2) 소관으로, 본 라운드는
     * 멱등 안내로 일관한다(에러 대신 사용자 안내 — 프론트에서 "이미 입실됨"으로 처리).
     */
    @Override
    @Transactional
    public AdminManagerDirectResponse managerDirectEnter(AdminManagerDirectParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.userCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 자동전이 지연평가 후 세션 소유/스코프 재검증(IDOR).
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        // D-2: 교육준비(OPENED) 상태에서만 입실 처리.
        if (!"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 정규직 대리입실 불가 상태 - sessionCd={}, status={}",
                    param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_040);
        }

        // PRAFTA-SUBCON-T5 F9: 앱 관리자 대리입실(P3)은 <b>자사 대상 전용</b>이다.
        //   요청서 §3.2 는 MANAGER_DIRECT(검색 직접입실)의 대상 확장을 <b>웹</b>으로만 명시했고,
        //   요청서 §4 는 지정/대상선택 UI 를 웹 전용으로 뒀다. 앱에는 대상 회사 선택 UI 가 없으므로
        //   타사 회사코드를 받아들이면 쓰이지 않는 공격 표면만 남는다 → targetCmpnyCd 수용을 제거한다.
        //   (앱의 타사 참여 경로는 P4 일용직 QR 입실이며 그쪽은 그대로 유지한다.)
        //   자사 대상이어도 공통 게이트는 그대로 통과시킨다(세션 존재/개설사 판정 단일 지점 유지).
        String targetCmpnyCd = param.gvCmpnyCd();
        SessionOwnerResult session = tbmSessionShareService.assertEntryAllowed(
                param.sessionCd(), targetCmpnyCd);
        boolean ownTarget = session.hostCmpnyCd().equals(targetCmpnyCd);

        // 노드관리자면 세션 사업장 기준 자기노드+자손 NODE_CD 집합으로 대상 정규직을 제한한다(E9 동형).
        // 읽기(E9 검색)/쓰기(E10 대리입실) 권한 비대칭 차단 — 서브트리 밖 정규직 대리입실 거부.
        List<String> scopedNodeCds = Collections.emptyList();
        if (!scope.companyWide()) {
            scopedNodeCds = adminScopeMapper.selectScopedNodeCds(
                    ScopedNodeQuery.of(param.gvCmpnyCd(), guard.siteCd(), param.gvUserCd()));
        }

        // 대상이 세션 사업장 소속 활성 정규직 + 노드 스코프 내인지 서버 재검증(IDOR — 임의 userCd 입실 차단).
        int valid = appAdminTbmMapper.countEntryTarget(
                AdminEntryTargetQuery.of(targetCmpnyCd, guard.siteCd(), "REGULAR", param.userCd(),
                        scope.companyWide(), scopedNodeCds, ownTarget));
        if (valid <= 0) {
            log.warn("TBM 정규직 대리입실 대상 부적합(사업장/노드 스코프 밖) - sessionCd={}, userCd={}",
                    param.sessionCd(), param.userCd());
            throw new ApiException(TbmErrorCode.TBM_403_040);
        }

        // UK 멱등: 이미 입실(DEL_YN 무관) 행이 있으면 UNIQUE 충돌 → 멱등 안내. 재입실 복구는 R-C(#D-RE2).
        // 출결행 CMPNY_CD/채번은 참석자 회사(=자사), INSERT 가드는 개설사 기준(T5).
        try {
            int inserted = appAdminTbmMapper.insertManagerDirectEntry(AdminManagerEnterCommand.of(
                    targetCmpnyCd, session.hostCmpnyCd(), param.sessionCd(), "REGULAR", param.userCd(),
                    "MANAGER_DIRECT", param.gvUserCd()));
            if (inserted == 0) {
                // TOCTOU: 가드~INSERT 사이 자동/동시 전이로 OPENED 이탈(D-2 우회 차단). INSERT...SELECT 가드 0건.
                log.warn("TBM 정규직 대리입실 상태 전이 충돌(교육준비 상태 아님) - sessionCd={}, userCd={}",
                        param.sessionCd(), param.userCd());
                throw new ApiException(TbmErrorCode.TBM_409_040);
            }
        } catch (DuplicateKeyException e) {
            log.info("TBM 정규직 대리입실 멱등(이미 입실/경합) - sessionCd={}, userCd={}",
                    param.sessionCd(), param.userCd());
            throw new ApiException(TbmErrorCode.TBM_409_041);
        }

        log.info("앱 관리자 TBM 정규직 대리입실 완료 - sessionCd={}, userCd={}, manager={}",
                param.sessionCd(), param.userCd(), param.gvUserCd());

        return AdminManagerDirectResponse.builder()
                .sessionCd(param.sessionCd())
                .userTypeCd("REGULAR")
                .userCd(param.userCd())
                .build();
    }

    // ============================ prafta-051 R-D 입실경로(일용직 QR 입실) ============================

    /**
     * E11 일용직 QR 입실(MANAGER_QR_SCAN). Flutter SCAN_QR 브리지가 전달한 QR raw 문자열(JSON)을 파싱해
     * 일용직 식별키(userCd)를 추출하고, 세션 OPENED(D-2) + 대상이 세션 사업장 소속 유효 일용직인지 서버 재검증(IDOR)
     * 후 TB_TBM_ATTENDANCE 에 입실 INSERT 한다. 비번/GPS 반경검증은 하지 않고(D-4), ENTRY_GPS_LAT/LON 은 세션의
     * 관리자 좌표를 감사용으로 복사한다(R-B 동형 INSERT 공유).
     *
     * <p>⚠️ 보안(#DF-1): QR 안의 cmpnyCd/siteCd 는 신뢰하지 않는다. 회사는 토큰 CMPNY, 대상 사업장은 세션 SITE 를
     * 단일 출처로 쓰고, QR 에 cmpnyCd/siteCd 가 들어 있으면 토큰/세션과 일치하는지만 추가 검증한다(불일치 시 거부).
     * 직접가입 QR 은 userCd 가 없으므로 userCd 누락/파싱 실패는 400(TBM_400_044).
     *
     * <p>일용직 유효성(web Tbm02.countEntryTarget DAILY 포팅): USE_YN='Y', ACCOUNT_STATUS='01',
     * WITHDRAWAL_DATE IS NULL, WORK_EXPIRE_DATE&gt;=오늘, 동일 회사/사업장. tb_daily_user 에는 NODE_CD 가
     * 없으므로([developer 확정] 스키마 확인) 노드 스코프는 적용하지 않고 사업장 단위로 검증한다.
     *
     * <p>UK(CMPNY,SESSION,USER_TYPE,USER) 충돌은 DuplicateKeyException 으로 멱등 안내(TBM_409_041).
     * INSERT...SELECT 의 STATUS_CD='OPENED' 가드로 TOCTOU(가드~INSERT 사이 자동/동시 전이) 를 차단한다(R-C 교훈).
     */
    @Override
    @Transactional
    public AdminQrScanResponse qrScanEnter(AdminQrScanParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.qrPayload())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 방어적 길이 상한(application/json 은 multipart 캡 미적용) — 정상 QR 은 ~3KB. 초과 시 파싱 전 거부.
        if (param.qrPayload().length() > QR_PAYLOAD_MAX_LENGTH) {
            log.warn("TBM 일용직 QR 입실 페이로드 길이 초과({}자) - sessionCd={}", param.qrPayload().length(), param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_400_044);
        }

        // QR 페이로드(JSON) 파싱 → 일용직 식별키 userCd 추출(필수). QR 의 cmpnyCd/siteCd 는 신뢰하지 않고 검증용으로만 본다.
        String qrUserCd;
        String qrCmpnyCd;
        String qrSiteCd;
        try {
            JsonNode node = objectMapper.readTree(param.qrPayload());
            qrUserCd = textOrNull(node, "userCd");
            qrCmpnyCd = textOrNull(node, "cmpnyCd");
            qrSiteCd = textOrNull(node, "siteCd");
        } catch (Exception e) {
            // 파싱 실패/형식오류(직접가입 QR 등) → 사용자 안내(평문 PII 미출력, payload 원문 로깅 금지).
            log.warn("TBM 일용직 QR 입실 페이로드 파싱 실패 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_400_044);
        }
        if (!StringUtils.hasText(qrUserCd)) {
            log.warn("TBM 일용직 QR 입실 식별키(userCd) 누락 - sessionCd={}", param.sessionCd());
            throw new ApiException(TbmErrorCode.TBM_400_044);
        }

        // 자동전이 지연평가 후 세션 소유/스코프 재검증(IDOR).
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        // D-2: 교육준비(OPENED) 상태에서만 입실 처리.
        if (!"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 일용직 QR 입실 불가 상태 - sessionCd={}, status={}",
                    param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_040);
        }

        // ===== PRAFTA-SUBCON-T5(P4 재설계) =====
        // 기존: QR 의 cmpnyCd 가 토큰 회사와 다르면 무조건 거부(타사 QR 차단).
        // 변경: 차단선을 "토큰 회사 일치" → "지정 체인 소속"으로 이동한다. QR 이 체인 밖 회사를 주장하면
        //       공통 게이트가 403(TBM_403_060)으로 막는다(QR 위·변조/타사 QR 주입 차단은 그대로 유지).
        String targetCmpnyCd = StringUtils.hasText(qrCmpnyCd) ? qrCmpnyCd : param.gvCmpnyCd();
        SessionOwnerResult session = tbmSessionShareService.assertEntryAllowed(
                param.sessionCd(), targetCmpnyCd);
        boolean ownTarget = session.hostCmpnyCd().equals(targetCmpnyCd);

        // QR 사업장 검증: 자사(개설사) 대상은 기존대로 세션 사업장과 일치 요구.
        //   타사 대상은 사업장 일치 검사를 하지 않는다 — 타사 사업장코드는 그 회사 네임스페이스라
        //   개설사 세션 사업장과 비교하는 것 자체가 무의미하다(plan D4).
        if (ownTarget && StringUtils.hasText(qrSiteCd) && !qrSiteCd.equals(guard.siteCd())) {
            log.warn("TBM 일용직 QR 입실 사업장 불일치 거부 - sessionCd={}, sessionSite={}",
                    param.sessionCd(), guard.siteCd());
            throw new ApiException(TbmErrorCode.TBM_403_040);
        }

        // 대상이 유효 일용직인지 서버 재검증(IDOR). tb_daily_user 는 NODE_CD 없음 → 사업장/회사 단위.
        //   만료/탈퇴/계정상태 조건은 그대로(신원 보증 책임은 고용 회사 — 요청서 §3.2).
        //   ※ 일용직 블랙리스트는 회사별 관리이므로 개설사 블랙리스트는 타사 일용직에 적용되지 않는다(의도된 동작).
        int valid = appAdminTbmMapper.countEntryTarget(
                AdminEntryTargetQuery.of(targetCmpnyCd, guard.siteCd(), "DAILY", qrUserCd,
                        true, Collections.emptyList(), ownTarget));
        if (valid <= 0) {
            log.warn("TBM 일용직 QR 입실 대상 부적합(사업장 밖/만료/탈퇴) - sessionCd={}, targetCmpny={}",
                    param.sessionCd(), targetCmpnyCd);
            throw new ApiException(TbmErrorCode.TBM_403_040);
        }

        // prafta-app-025 J1-7 작업 D: 출근 선행 가드. 당일 해당 일용직의 출근 기록이 없으면 입실 차단.
        //   T5(plan D6): 자사 대상은 기존대로 세션 사업장 기준. 타사 대상은 그 회사의 (미러) 사업장에
        //   출근이 기록되므로 개설사 사업장코드로 매칭할 수 없다 → 회사 단위로 검사한다(가드 자체는 유지).
        String todayYmd = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int todayCheckIn = appAdminTbmMapper.countTodayCheckIn(
                targetCmpnyCd, guard.siteCd(), qrUserCd, todayYmd, ownTarget);
        if (todayCheckIn <= 0) {
            log.warn("TBM 일용직 QR 입실 거부(당일 출근 기록 없음) - sessionCd={}, targetCmpny={}",
                    param.sessionCd(), targetCmpnyCd);
            throw new ApiException(TbmErrorCode.TBM_409_044);
        }

        // UK 멱등: 이미 입실 행이 있으면 UNIQUE 충돌 → 멱등 안내. INSERT...SELECT 가드(OPENED)로 TOCTOU 차단.
        // 출결행 CMPNY_CD/채번은 참석자 회사, INSERT 가드는 개설사 기준(T5).
        try {
            int inserted = appAdminTbmMapper.insertManagerDirectEntry(AdminManagerEnterCommand.of(
                    targetCmpnyCd, session.hostCmpnyCd(), param.sessionCd(), "DAILY", qrUserCd,
                    "MANAGER_QR_SCAN", param.gvUserCd()));
            if (inserted == 0) {
                log.warn("TBM 일용직 QR 입실 상태 전이 충돌(교육준비 상태 아님) - sessionCd={}, userCd={}",
                        param.sessionCd(), qrUserCd);
                throw new ApiException(TbmErrorCode.TBM_409_040);
            }
        } catch (DuplicateKeyException e) {
            log.info("TBM 일용직 QR 입실 멱등(이미 입실/경합) - sessionCd={}, userCd={}",
                    param.sessionCd(), qrUserCd);
            throw new ApiException(TbmErrorCode.TBM_409_041);
        }

        log.info("앱 관리자 TBM 일용직 QR 입실 완료 - sessionCd={}, userCd={}, targetCmpny={}, manager={}",
                param.sessionCd(), qrUserCd, targetCmpnyCd, param.gvUserCd());

        // 스캔 결과 화면의 회사명은 스캔 주체(개설사) 기준 1차 relabel 값만 내려준다(요청서 §3.2 ①).
        String affilCmpnyNm = tbmSessionShareService.resolveTier1LabelMap(param.sessionCd())
                .get(targetCmpnyCd);

        return AdminQrScanResponse.builder()
                .sessionCd(param.sessionCd())
                .userTypeCd("DAILY")
                .userCd(qrUserCd)
                .affilCmpnyNm(affilCmpnyNm)
                .build();
    }

    /** QR JSON 노드에서 텍스트 필드를 안전 추출(없거나 null 이면 null). */
    private String textOrNull(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    // ============================ prafta-051 R-C 이탈자 내보내기(입실취소) ============================

    /**
     * prafta-051 E13 입실취소(GPS 이탈자 내보내기). D-3: 교육 미시작(OPENED) 단계의 단순 입실취소다
     * (강제퇴실 MANAGER_FORCED/미이수와 구분 — IN_PROGRESS 강제퇴실은 forceExitAttendee 소관).
     *
     * <p>#D-RE2 = 물리삭제: 출결행을 물리 DELETE 한다(사용자 leaveBefore deleteAttendance 동형).
     * 소프트삭제가 아니므로 UK(CMPNY,SESSION,USER_TYPE,USER) 잔여행이 남지 않아, 취소 대상은
     * 재입실(본인 입실/대리입실) 시 UK 충돌 없이 신규 INSERT 된다(R-B managerDirectEnter 의 UK 멱등과 정합).
     *
     * <p>가드: 자동전이 지연평가(D-1) 후 세션 OPENED + 스코프 재검증(IDOR). 매퍼 DELETE 는 세션 OPENED
     * 서브쿼리로 TOCTOU(가드~삭제 사이 자동/동시 전이) 를 차단한다. 영향 0건이면 멱등 안내(이미 취소/없음).
     */
    @Override
    @Transactional
    public void cancelEntry(AdminCancelEntryParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.attendanceCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 자동전이 지연평가 후 세션 소유/스코프 재검증(IDOR).
        evaluateAutoStart(param.gvCmpnyCd(), param.sessionCd());

        AdminSessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
        verifyScope(scope, guard.siteCd(), guard.managerNodeCd(), param.gvCmpnyCd(), param.gvUserCd());

        // D-3: 교육준비(OPENED) 상태에서만 입실취소 허용(교육 미시작 단계).
        if (!"OPENED".equals(guard.statusCd())) {
            log.warn("TBM 입실취소 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_042);
        }

        AdminCancelEntryCommand cmd = AdminCancelEntryCommand.of(
                param.sessionCd(), param.attendanceCd(), param.gvCmpnyCd());

        // 물리삭제 후에는 행이 사라지므로, 동일 트랜잭션 내에서 삭제 직전 대상 식별 코드값을 스냅샷한다(감사 추적용).
        AdminCancelEntrySnapshotResult snapshot = appAdminTbmMapper.selectCancelEntrySnapshot(cmd);

        int deleted = appAdminTbmMapper.deleteCancelEntry(cmd);
        if (deleted == 0) {
            // 멱등: 이미 취소/없음, 이미 종료, 또는 TOCTOU 자동전이로 OPENED 이탈(서브쿼리 가드 0건).
            log.warn("TBM 입실취소 대상 없음/이미 취소/상태 전이 - sessionCd={}, attendanceCd={}",
                    param.sessionCd(), param.attendanceCd());
            throw new ApiException(TbmErrorCode.TBM_409_043);
        }

        // 누가(byManager) 누구를(userCd/userTypeCd) 취소했는지 코드값만 기록(평문 이름/좌표 미포함).
        log.info("앱 관리자 TBM 입실취소(물리삭제) 완료 - sessionCd={}, attendanceCd={}, targetUserCd={}, targetUserTypeCd={}, byManager={}",
                param.sessionCd(), param.attendanceCd(),
                snapshot != null ? snapshot.userCd() : null,
                snapshot != null ? snapshot.userTypeCd() : null,
                param.gvUserCd());
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

    /** 위험성평가 매핑 다건 INSERT(DISPLAY_ORDER 자동 부여, 옵션). 삽입 전 사업장 정합·실존 검증. */
    private void insertRisks(List<AdminSessionRiskModel> risks, String sessionCd, String sessionSiteCd,
            String gvCmpnyCd, String gvUserCd) {
        if (risks == null) {
            return;
        }
        validateRiskLinks(risks, sessionSiteCd, gvCmpnyCd);
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

    /**
     * 세션-위험성평가 연계 서버 검증(2026-07-16 보안 보강 — 웹 Tbm02ServiceImpl.validateRiskLinks 미러.
     * 사용자 결정: 연계는 사업장 단위).
     *
     * <ul>
     *   <li>(a) 제출된 각 평가의 siteCd 가 세션 SITE_CD 와 일치해야 한다(교차 사업장 매핑 불허).</li>
     *   <li>(b) 제출 키(siteCd/processCd/assessmentCd)가 TB_RISK_ASSESSMENT 에 실존해야 한다
     *       (중복 제거 후 카운트 쿼리 1방 — 건수 불일치 시 dangling 키 포함으로 판정).</li>
     * </ul>
     * insert 대상이 아닌 행(processCd/assessmentCd 공백 — 기존 skip 규칙)은 검증도 생략한다.
     * 위반 시 TBM_400_016.
     */
    private void validateRiskLinks(List<AdminSessionRiskModel> risks, String sessionSiteCd, String gvCmpnyCd) {
        List<AdminSessionRiskModel> targets = new ArrayList<>();
        Set<String> distinctKeys = new LinkedHashSet<>();
        for (AdminSessionRiskModel model : risks) {
            if (!StringUtils.hasText(model.getProcessCd()) || !StringUtils.hasText(model.getAssessmentCd())) {
                continue;
            }
            if (!sessionSiteCd.equals(model.getSiteCd())) {
                log.warn("앱 관리자 TBM 세션-위험성평가 사업장 불일치 - sessionSiteCd={}, riskSiteCd={}, assessmentCd={}",
                        sessionSiteCd, model.getSiteCd(), model.getAssessmentCd());
                throw new ApiException(TbmErrorCode.TBM_400_016);
            }
            if (distinctKeys.add(model.getSiteCd() + "|" + model.getProcessCd() + "|" + model.getAssessmentCd())) {
                targets.add(model);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        int found = appAdminTbmMapper.countRiskAssessments(gvCmpnyCd, targets);
        if (found != targets.size()) {
            log.warn("앱 관리자 TBM 세션-위험성평가 미존재 키 포함 - submitted={}, found={}", targets.size(), found);
            throw new ApiException(TbmErrorCode.TBM_400_016);
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

    /** 입실비번 노출(prafta-051): OPENED 이상(OPENED/IN_PROGRESS/COMPLETED) + 관리자. */
    private boolean isEntryPwdVisible(String statusCd, String authCd, boolean companyWide) {
        boolean entryStatus = "OPENED".equals(statusCd)
                || "IN_PROGRESS".equals(statusCd)
                || "COMPLETED".equals(statusCd);
        return entryStatus && isManagerRole(authCd, companyWide);
    }

    /** 종료비번 노출(prafta-051): COMPLETED 이상 + 관리자. */
    private boolean isExitPwdVisible(String statusCd, String authCd, boolean companyWide) {
        return "COMPLETED".equals(statusCd) && isManagerRole(authCd, companyWide);
    }

    /** 비번 노출 가능 권한(전사 master/safe + 공통관리/노드관리자). */
    private boolean isManagerRole(String authCd, boolean companyWide) {
        return companyWide || AuthRoleUtils.canManageCommon(authCd) || AuthRoleUtils.isManager(authCd);
    }

    /**
     * prafta-051 §2: 15분 자동 교육시작 지연평가(멱등). OPENED + PREP_START_AT 만료 세션을
     * IN_PROGRESS 로 전이한다. 영향행 0 = 이미 전이됐거나 아직 시간 전(no-op).
     * 조회/시작/연장 진입부에서 호출(트랜잭션 경계는 호출 메서드에 위임 — #DF-4 단순 분리 호출).
     */
    private void evaluateAutoStart(String cmpnyCd, String sessionCd) {
        int n = appAdminTbmMapper.evaluateAutoStart(cmpnyCd, sessionCd, prepAutoStartMinutes);
        if (n > 0) {
            // 실제 0→1 전이가 일어난 경우에만(멱등 UPDATE affected==1) 교육 시작 통보 적재.
            // WHERE STATUS_CD='OPENED' 가드로 n>0 은 본 호출에서 단 한 번만 성립(수동 start/동시 진입과 경합 안전).
            log.info("TBM 교육준비 {}분 경과 자동 교육시작 전이 - sessionCd={}", prepAutoStartMinutes, sessionCd);

            // PRAFTA-APP-021-3b(W3 자동시작): 자동전이 세션도 입실 참석자에게 시작 PUSH 적재.
            // dedupKey(TBM_STARTED_{sessionCd}_{userCd}) 멱등 → 수동/자동 동시 발생해도 입실자당 1회만 발송.
            // actor(INSERT_NO)=개설자(MANAGER_USER_CD). 전이 영향 없도록 try-catch 격리.
            try {
                AdminSessionGuardResult guard = appAdminTbmMapper.selectSessionGuard(
                        AdminSessionDetailQuery.of(sessionCd, cmpnyCd));
                if (guard != null) {
                    tbmEventNotiService.notifyTbmStarted(
                            cmpnyCd, guard.siteCd(), sessionCd, guard.managerUserCd());
                }
            } catch (Exception e) {
                log.error("TBM 자동 교육시작 통보 PUSH 적재 hook 실패(전이 영향 없음). sessionCd={}", sessionCd, e);
            }
        }
    }

    /**
     * 자동 교육시작 예정시각 산출(=PREP_START_AT + 자동시작분, UTC 절대시각 'Z' 접미사).
     * OPENED 이며 prepStartAtEpoch 존재 시에만 산출. 응답 포맷('yyyy-MM-dd HH:mm:ssZ')은 종전과 동일.
     *
     * <p>★저장 타임존을 코드에서 가정하지 말 것: 2026-08-23엔 "DB=UTC" 전제로 벽시계에 Z 를 붙였는데,
     * 2026-08-30 DB 세션 KST 전환 후 예정시각이 9시간 미래로 밀렸다(카운트다운 555분 결함).
     * 매퍼의 {@code UNIX_TIMESTAMP(PREP_START_AT)}(세션 타임존으로 해석된 epoch = 절대시각)를 쓰면
     * DB 가 어느 타임존이든 항상 올바르다. (웹 Tbm02ServiceImpl 동일 수정 — 미러 유지)
     */
    private String computePrepAutoStartAt(String statusCd, Long prepStartAtEpoch) {
        // KST 전환(2026-08-30) 대응: 벽시계+Z(="DB=UTC 저장" 가정) → epoch 기반으로 교체.
        // UNIX_TIMESTAMP(PREP_START_AT)는 세션 타임존으로 해석된 절대시각이라 저장 TZ 가정이 불필요
        // (전환 후 예정시각이 9시간 미래로 밀리던 카운트다운 555분 결함 수정 — 웹 Tbm02ServiceImpl 미러).
        if (!"OPENED".equals(statusCd) || prepStartAtEpoch == null) {
            return null;
        }
        return java.time.Instant.ofEpochSecond(prepStartAtEpoch)
                .plus(java.time.Duration.ofMinutes(prepAutoStartMinutes))
                .atOffset(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "Z";
    }

    /**
     * 교육 인정시간(분) 검증(공유계약: 1~60 정수, 웹 Tbm02ServiceImpl 과 동일 규칙).
     *
     * <p>{@code required=false}: null 허용(개설/수정). 값이 있으면 1~60 검증.
     * {@code required=true}: null도 거부(교육준비 시작 — 미입력이면 TBM_400_015).
     */
    private void validateEduMinutes(Integer eduMinutes, boolean required) {
        if (eduMinutes == null) {
            if (required) {
                log.warn("TBM 교육시간 미입력(교육준비 필수)");
                throw new ApiException(TbmErrorCode.TBM_400_015);
            }
            return;
        }
        if (eduMinutes < EDU_MINUTES_MIN || eduMinutes > EDU_MINUTES_MAX) {
            log.warn("TBM 교육시간 범위 벗어남 - eduMinutes={}", eduMinutes);
            throw new ApiException(TbmErrorCode.TBM_400_015);
        }
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

    /**
     * GPS좌표-암호화-전환-06: 관리자 좌표 문자열 → scale 7 정규화 → AES-GCM 암호문.
     * 공백/빈값은 null(기존 normalize 트림 규칙 미러). 숫자 파싱 실패는 기존 GPS 오류 체계(TBM_400_012)로
     * 거부한다(현재도 DB decimal 변환 실패로 거부되던 입력 — 오류 지점만 앞당김). 좌표값 로그 출력 금지.
     */
    private String encryptGpsOrReject(String raw) {
        try {
            return gpsCoordCrypto.encryptString(raw);
        } catch (NumberFormatException e) {
            log.warn("TBM 관리자 좌표 숫자 형식 오류(값 미출력)");
            throw new ApiException(TbmErrorCode.TBM_400_012);
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
            throw new ApiException(TbmErrorCode.TBM_400_050);
        }
        // 자료 타입(COM003) 유효성 — 회사 코드그룹에 존재하는지 확인.
        if (!StringUtils.hasText(param.mtrlType()) || !isValidMaterialType(param.gvCmpnyCd(), param.mtrlType())) {
            throw new ApiException(TbmErrorCode.TBM_400_051);
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
                throw new ApiException(TbmErrorCode.TBM_403_050);
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
                throw new ApiException(TbmErrorCode.TBM_403_050);
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
