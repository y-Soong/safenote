package com.prafta.app.safety.admin.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.safety.admin.application.command.RiskStatusCommand;
import com.prafta.app.safety.admin.application.param.InspectionDetailParam;
import com.prafta.app.safety.admin.application.param.InspectionListParam;
import com.prafta.app.safety.admin.application.param.RiskDetailParam;
import com.prafta.app.safety.admin.application.param.RiskFindingListParam;
import com.prafta.app.safety.admin.application.param.RiskStatusParam;
import com.prafta.app.safety.admin.application.query.InspectionDetailQuery;
import com.prafta.app.safety.admin.application.query.InspectionListQuery;
import com.prafta.app.safety.admin.application.query.RiskDetailQuery;
import com.prafta.app.safety.admin.application.query.RiskFindingListQuery;
import com.prafta.app.safety.admin.dto.response.InspectionDetailResponse;
import com.prafta.app.safety.admin.dto.response.InspectionListResponse;
import com.prafta.app.safety.admin.dto.response.RiskDetailResponse;
import com.prafta.app.safety.admin.dto.response.RiskFindingListResponse;
import com.prafta.app.safety.admin.mapper.AppAdminSafetyMapper;
import com.prafta.app.safety.admin.result.InspectionAnswerResult;
import com.prafta.app.safety.admin.result.InspectionPointResult;
import com.prafta.app.safety.admin.result.RiskAssessmentResult;
import com.prafta.app.safety.admin.result.RiskStatusMetaResult;
import com.prafta.app.safety.admin.service.AppAdminSafetyService;
import com.prafta.common.error.safety.SafetyErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 안전 관리(순회점검 결과/위험성평가) 서비스 구현 (prafta-app-025 J1-6).
 *
 * <p>웹 chkLst03/risk03 SQL 을 앱 매퍼로 포팅하되 사업장/권한 스코프를 서버에서 재강제한다(A-1 비상속).
 *    SAFETY 진입식(AdminAccessResolver §3.2) = master ∥ safe ∥ nodeAdmin-in-site. 순회점검/위험성평가
 *    테이블에 NODE_CD 가 없어 노드 세분화가 불가하므로 노드관리자 판정은 "현재 사업장 tb_user_site_auth 매핑"
 *    으로 입자한다(웹 chkLst03 SITE_LIST/ risk03 siteCd 필터와 동일 입자).
 *
 * <p>식별자(cmpny/user/auth)는 JWT 클레임에서만 도출하고, 리소스 사업장(siteCd)은 화면이 전달 가능하나
 *    멤버십을 재검증한다(master/safe 전사 예외). 리소스 키(chkptCd/assessmentCd)는 CMPNY+SITE WHERE 로 강제.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminSafetyServiceImpl implements AppAdminSafetyService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

    // SYS011 진행상태(라이브 DB 확인): 001 검토요청 / 002 개선예정 / 003 개선완료 / 004 미처리대상.
    private static final String STATUS_REQUESTED = "001"; // 검토요청
    private static final String STATUS_IN_PROG   = "002"; // 개선예정(검토 착수 + 임시조치)
    private static final String STATUS_DONE       = "003"; // 개선완료
    private static final String STATUS_DISCARDED  = "004"; // 미처리대상(폐기)

    private final AppAdminSafetyMapper mapper;
    private final AdminScopeMapper adminScopeMapper;

    // ============================ 진입/사업장 게이트 ============================

    /**
     * SAFETY 진입 + 사업장 접근 권위 확정(A-1 비상속, 형제 SITE_OPS 와 동형 2단 게이트).
     *
     * <p>AdminAccessResolver §3.2 SAFETY 활성식 = master ∥ safe ∥ nodeAdmin-in-site (hr 제외).
     * 두 단계로 분리 강제한다:
     * <ol>
     *   <li><b>역할 게이트(관리자 자격)</b>: master ∥ safe(canManageCommon) 이거나, 현재 사업장 기준
     *       노드 정/부 관리자(selectScopedNodeCds 비어있지 않음)일 때만 통과. 둘 다 아니면 403.
     *       — 단순 사업장 멤버십(평직원 본인 사업장 행 보유)으로는 통과하지 않는다(권한 상승 차단).
     *       — hr 은 SAFETY 전사 통과 대상이 아니므로 canManageAllNodes(master/hr/safe)가 아니라
     *         canManageCommon(master/safe)을 역할 게이트로 사용한다.</li>
     *   <li><b>사업장 멤버십(IDOR)</b>: 위 통과 후, 화면이 보낸 siteCd 가 토큰 사용자 접근 가능 사업장인지
     *       countUserSiteAuth(USE_YN='Y')로 재검증한다(아니면 403). 노드관리자라면 selectScopedNodeCds 가
     *       해당 사업장 기준 산출된 것이므로 자기 사업장만 통과된다.</li>
     * </ol>
     * <ul>
     *   <li>999999(미부여) → 403.</li>
     *   <li>사업장 권위 = 요청 siteCd(있으면) 또는 토큰 gv_siteCd. 공백이면 403.</li>
     * </ul>
     * @return 권위 확정된 사업장코드(이후 모든 쿼리 스코프 키).
     */
    private String resolveSiteScope(String cmpnyCd, String userCd, String siteCd, String authCd, String reqSiteCd) {
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("안전 관리 접근 차단(권한 없음) - authCd={}", authCd);
            throw new ApiException(SafetyErrorCode.SAFETY_403_001);
        }
        String effectiveSite = StringUtils.hasText(reqSiteCd) ? reqSiteCd : siteCd;
        if (!StringUtils.hasText(effectiveSite)) {
            log.warn("안전 관리 사업장 미지정 - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(SafetyErrorCode.SAFETY_403_001);
        }

        // 1) 역할 게이트(관리자 자격): master/safe(전사) 가 아니면, 현재 사업장 노드관리자(자기노드+자손 비어있지 않음)여야 통과.
        //    AdminAccessResolver §3.2 SAFETY = master ∥ safe ∥ nodeAdmin-in-site (hr 제외)와 정합.
        boolean companyWide = AuthRoleUtils.canManageCommon(authCd);   // master ∥ safe
        if (!companyWide) {
            List<String> scopedNodeCds = adminScopeMapper.selectScopedNodeCds(
                    ScopedNodeQuery.of(cmpnyCd, effectiveSite, userCd));
            if (scopedNodeCds == null || scopedNodeCds.isEmpty()) {
                log.warn("안전 관리 진입 권한 없음(노드관리자 아님) - userCd={}, authCd={}, siteCd={}",
                        userCd, authCd, effectiveSite);
                throw new ApiException(SafetyErrorCode.SAFETY_403_001);
            }
        }

        // 2) 사업장 멤버십(IDOR): 화면이 보낸 siteCd 가 토큰 사용자 접근 가능 사업장인지 재검증(전사역할도 검증).
        if (mapper.countUserSiteAuth(cmpnyCd, userCd, effectiveSite) == 0) {
            log.warn("안전 관리 사업장 권한 없음(IDOR 차단) - userCd={}, authCd={}, siteCd={}",
                    userCd, authCd, effectiveSite);
            throw new ApiException(SafetyErrorCode.SAFETY_403_001);
        }
        return effectiveSite;
    }

    // ============================ H1 순회점검 결과 리스트 ============================

    @Override
    public InspectionListResponse selectInspectionList(InspectionListParam param) {
        String siteCd = resolveSiteScope(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd(), param.siteCd());

        String workMonth = StringUtils.hasText(param.workMonth())
                ? param.workMonth()
                : LocalDate.now().format(YM);

        List<InspectionPointResult> points = nullSafe(mapper.selectInspectionList(
                new InspectionListQuery(param.gvCmpnyCd(), siteCd, workMonth)));

        log.info("앱 관리자 순회점검 결과 리스트 - cmpnyCd={}, siteCd={}, workMonth={}, 포인트={}건",
                param.gvCmpnyCd(), siteCd, workMonth, points.size());

        return InspectionListResponse.builder().points(points).build();
    }

    // ============================ H2 순회점검 상세 ============================

    @Override
    public InspectionDetailResponse selectInspectionDetail(InspectionDetailParam param) {
        String siteCd = resolveSiteScope(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd(), param.siteCd());

        // 순회점검 사진 FILE_TYPE='001'(일일점검 — 웹 chkLst03 InspectAnswerQuery 하드코딩 동일).
        InspectionDetailQuery query = new InspectionDetailQuery(
                param.gvCmpnyCd(), siteCd, param.chkLstType(), param.chkptCd(), param.workMonth(), "001");

        List<InspectionAnswerResult> answers = nullSafe(mapper.selectInspectionDetailAnswers(query));

        log.info("앱 관리자 순회점검 상세 - cmpnyCd={}, siteCd={}, chkptCd={}, workMonth={}, 답변={}건",
                param.gvCmpnyCd(), siteCd, param.chkptCd(), param.workMonth(), answers.size());

        return InspectionDetailResponse.builder().answers(answers).build();
    }

    // ============================ H3 위험성평가 목록 ============================

    @Override
    public RiskFindingListResponse selectRiskFindings(RiskFindingListParam param) {
        String siteCd = resolveSiteScope(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd(), param.siteCd());

        RiskFindingListQuery query = new RiskFindingListQuery(
                param.gvCmpnyCd(), siteCd, param.assessmentStatus(), param.processCd(), param.riskTypeCd());

        List<RiskAssessmentResult> findings = nullSafe(mapper.selectRiskFindingList(query));

        log.info("앱 관리자 위험성평가 목록 - cmpnyCd={}, siteCd={}, status={}, 건수={}",
                param.gvCmpnyCd(), siteCd, param.assessmentStatus(), findings.size());

        return RiskFindingListResponse.builder().findings(findings).build();
    }

    // ============================ H4 위험성평가 상세 ============================

    @Override
    public RiskDetailResponse selectRiskDetail(RiskDetailParam param) {
        String siteCd = resolveSiteScope(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd(), param.siteCd());

        RiskAssessmentResult detail = mapper.selectRiskFindingDetail(new RiskDetailQuery(
                param.gvCmpnyCd(), siteCd, param.processCd(), param.assessmentCd()));
        if (detail == null) {
            // 사업장 스코프 밖/존재X → 일반화 404(IDOR 차단).
            log.warn("앱 관리자 위험성평가 상세 없음 - siteCd={}, processCd={}, assessmentCd={}",
                    siteCd, param.processCd(), param.assessmentCd());
            throw new ApiException(SafetyErrorCode.SAFETY_404_001);
        }

        return RiskDetailResponse.builder().detail(detail).build();
    }

    // ============================ H5 위험성평가 상태전환 ============================

    @Override
    @Transactional
    public void changeRiskStatus(RiskStatusParam param) {
        String siteCd = resolveSiteScope(
                param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd(), param.siteCd());

        String target = param.targetStatus();
        // 목표 상태 화이트리스트(전이 가능한 목표만): 002/003/004.
        if (!STATUS_IN_PROG.equals(target) && !STATUS_DONE.equals(target) && !STATUS_DISCARDED.equals(target)) {
            throw new ApiException(SafetyErrorCode.SAFETY_422_001);
        }

        // 현재 상태 조회(사업장 스코프). 없으면 404(IDOR 차단).
        RiskStatusMetaResult meta = mapper.selectRiskFindingMeta(new RiskDetailQuery(
                param.gvCmpnyCd(), siteCd, param.processCd(), param.assessmentCd()));
        if (meta == null) {
            log.warn("앱 관리자 위험성평가 전환 대상 없음 - siteCd={}, processCd={}, assessmentCd={}",
                    siteCd, param.processCd(), param.assessmentCd());
            throw new ApiException(SafetyErrorCode.SAFETY_404_001);
        }
        String current = meta.assessmentStatus();

        // 전이표 검증(허용 전이 외 전부 422):
        //   001 -> 002(임시조치) / 001 -> 004(폐기) / 002 -> 003(완료) / 002 -> 004(폐기).
        if (!isAllowedTransition(current, target)) {
            log.warn("앱 관리자 위험성평가 허용되지 않는 전이 - {} -> {}", current, target);
            throw new ApiException(SafetyErrorCode.SAFETY_422_001);
        }

        // 002 전환은 개선예정일 + 임시조치 필수(서버 강제 — 프론트 검증 재확인).
        String revalDateYmd = null;
        if (STATUS_IN_PROG.equals(target)) {
            if (!StringUtils.hasText(param.revalDate()) || !StringUtils.hasText(param.revalBeforeDesc())) {
                throw new ApiException(SafetyErrorCode.SAFETY_400_001);
            }
            revalDateYmd = normalizeYmd(param.revalDate());
            if (revalDateYmd == null) {
                throw new ApiException(SafetyErrorCode.SAFETY_400_001);
            }
        }

        // 현재 상태 가드 WHERE 로 동시 전환 직렬화. 영향 0건이면 이미 다른 상태로 전환됨 → 409.
        RiskStatusCommand command = new RiskStatusCommand(
                param.gvCmpnyCd(), siteCd, param.processCd(), param.assessmentCd(),
                target, current, revalDateYmd, param.revalBeforeDesc(), param.gvUserCd());

        int updated = mapper.updateRiskStatus(command);
        if (updated == 0) {
            log.warn("앱 관리자 위험성평가 동시 전환 충돌 - siteCd={}, assessmentCd={}, {} -> {}",
                    siteCd, param.assessmentCd(), current, target);
            throw new ApiException(SafetyErrorCode.SAFETY_409_001);
        }

        log.info("앱 관리자 위험성평가 상태전환 완료 - siteCd={}, assessmentCd={}, {} -> {}",
                siteCd, param.assessmentCd(), current, target);
    }

    // ============================ 보조 ============================

    /** 모바일 MVP 허용 전이(단방향 진행 + 폐기). 풀 재평가/역방향은 웹 위임. */
    private boolean isAllowedTransition(String current, String target) {
        if (current == null) {
            return false;
        }
        if (STATUS_REQUESTED.equals(current)) {
            return STATUS_IN_PROG.equals(target) || STATUS_DISCARDED.equals(target);
        }
        if (STATUS_IN_PROG.equals(current)) {
            return STATUS_DONE.equals(target) || STATUS_DISCARDED.equals(target);
        }
        // 003/004 는 모바일 전환 불가(읽기 전용 — 웹에서 진행).
        return false;
    }

    /** 'YYYY-MM-DD' 또는 'YYYYMMDD' → 'YYYYMMDD'(8자리 숫자) 정규화. 형식 오류면 null. */
    private String normalizeYmd(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replace("-", "").trim();
        if (digits.length() != 8 || !digits.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return digits;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
