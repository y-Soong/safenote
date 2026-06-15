package com.prafta.app.admin.dashboard.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.app.admin.access.application.query.AdminAccessQuery;
import com.prafta.app.admin.access.mapper.AppAdminAccessMapper;
import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.admin.dashboard.application.param.DashboardSummaryParam;
import com.prafta.app.admin.dashboard.application.query.AttdCountQuery;
import com.prafta.app.admin.dashboard.application.query.SafetyCountQuery;
import com.prafta.app.admin.dashboard.dto.response.DashboardSummaryResponse;
import com.prafta.app.admin.dashboard.mapper.AppAdminDashboardMapper;
import com.prafta.app.admin.dashboard.result.AttdCountResult;
import com.prafta.app.admin.dashboard.result.PatrolCountResult;
import com.prafta.app.admin.dashboard.service.AppAdminDashboardService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * J1-10 (B-5): 관리자 대시보드 요약 서비스 구현(조회 전용).
 *
 * <p>한 EP 에서 진입 1차 게이트(2-1)와 두 축 위젯 게이트(근태=노드축 2-2 / 안전3종=사업장축 2단 2-3)를 각각
 * 평가한다. 진입만 통과하면 200 을 반환하고, 각 위젯의 권한은 available 플래그로 표현한다(C1: 클라가 역할로
 * 분기하지 않고 서버 산출 플래그만 신뢰).
 *
 * <p>식별자(cmpny/user/auth)는 JWT 클레임에서만 도출하고, siteCd 는 현재 선택 사업장(요청 또는 토큰)이되
 * 멤버십을 재검증한다(IDOR). 카운트는 PII 없이 숫자만 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminDashboardServiceImpl implements AppAdminDashboardService {

    private final AppAdminDashboardMapper mapper;
    private final AdminScopeMapper adminScopeMapper;       // 노드 자손 전개(재귀 CTE) 재사용
    private final AppAdminAccessMapper appAdminAccessMapper; // 진입 1차 게이트(전사 노드관리자 존재) 재사용

    @Override
    public DashboardSummaryResponse selectSummary(DashboardSummaryParam param) {
        String cmpnyCd = param.gvCmpnyCd();
        String userCd = param.gvUserCd();
        String authCd = param.gvAuthCd();

        // 현재 선택 사업장 = 요청 siteCd(있으면) 또는 토큰 gv_siteCd.
        String effectiveSite = StringUtils.hasText(param.siteCd()) ? param.siteCd() : param.gvSiteCd();

        // ── 진입 1차 게이트(2-1): 999999/siteCd 공백 → 403. master∥hr∥safe∥(전사)노드관리자 아니면 403. ──
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("대시보드 진입 차단(권한 없음) - authCd={}", authCd);
            throw new ApiException(CommonErrorCode.COMMON_403_001);
        }
        if (!StringUtils.hasText(effectiveSite)) {
            log.warn("대시보드 진입 차단(사업장 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(CommonErrorCode.COMMON_403_001);
        }
        boolean roleManager = AuthRoleUtils.AUTH_MASTER.equals(authCd)
                || AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)
                || AuthRoleUtils.AUTH_SAFETY_MANAGER.equals(authCd);
        boolean nodeAdminAnySite =
                appAdminAccessMapper.existsNodeAdminAnySite(AdminAccessQuery.ofCompany(cmpnyCd, userCd)) > 0;
        if (!roleManager && !nodeAdminAnySite) {
            log.warn("대시보드 진입 차단(관리자 아님) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(CommonErrorCode.COMMON_403_001);
        }

        // 현재 사업장 노드 스코프(자기노드+자손). 근태/안전 두 게이트의 공통 입력(현재 사업장 기준).
        List<String> scopedNodeCds = nullSafe(adminScopeMapper.selectScopedNodeCds(
                ScopedNodeQuery.of(cmpnyCd, effectiveSite, userCd)));
        boolean nodeAdminInSite = !scopedNodeCds.isEmpty();

        // ── 위젯별 게이트 평가 ──
        boolean attdAvailable = resolveAttdGate(authCd, nodeAdminInSite);
        boolean safetyAvailable = resolveSafetyGate(cmpnyCd, userCd, authCd, effectiveSite, nodeAdminInSite);

        // ── 근태 카운트(available 시에만 쿼리) ──
        DashboardSummaryResponse.Attendance attendance = buildAttendance(
                attdAvailable, cmpnyCd, authCd, effectiveSite, scopedNodeCds);

        // ── 안전 3종 카운트(available 시에만 쿼리) — 한 게이트 공유 ──
        DashboardSummaryResponse.Patrol patrol;
        DashboardSummaryResponse.Risk risk;
        DashboardSummaryResponse.NearMiss nearMiss;
        if (safetyAvailable) {
            String todayYmd = mapper.selectTodayYmd();
            SafetyCountQuery sq = new SafetyCountQuery(cmpnyCd, effectiveSite, todayYmd);

            PatrolCountResult patrolCnt = mapper.selectPatrolCount(sq);
            patrol = DashboardSummaryResponse.Patrol.builder()
                    .available(true)
                    .targetCnt(patrolCnt == null ? 0 : patrolCnt.targetCnt())
                    .completedCnt(patrolCnt == null ? 0 : patrolCnt.completedCnt())
                    .build();
            risk = DashboardSummaryResponse.Risk.builder()
                    .available(true)
                    .pendingCnt(mapper.selectRiskPendingCount(sq))
                    .build();
            nearMiss = DashboardSummaryResponse.NearMiss.builder()
                    .available(true)
                    .newCnt(mapper.selectNearMissNewCount(sq))
                    .build();
        } else {
            patrol = DashboardSummaryResponse.Patrol.builder().available(false).build();
            risk = DashboardSummaryResponse.Risk.builder().available(false).build();
            nearMiss = DashboardSummaryResponse.NearMiss.builder().available(false).build();
        }

        log.info("관리자 대시보드 요약 조회 - cmpnyCd={}, siteCd={}, authCd={}, attd={}, safety={}",
                cmpnyCd, effectiveSite, authCd, attdAvailable, safetyAvailable);

        return DashboardSummaryResponse.builder()
                .attendance(attendance)
                .patrol(patrol)
                .risk(risk)
                .nearMiss(nearMiss)
                .build();
    }

    // ============================ 게이트 산출 ============================

    /**
     * 근태 위젯 게이트(2-2, 노드축 — ATTD_DETAIL 활성식과 동형).
     * <p>master/hr(전사) ∥ 현재 사업장 노드관리자. safe 단독은 ⛔(근태 권한 없음).
     */
    private boolean resolveAttdGate(String authCd, boolean nodeAdminInSite) {
        boolean companyWide = AuthRoleUtils.AUTH_MASTER.equals(authCd)
                || AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd);
        return companyWide || nodeAdminInSite;
    }

    /**
     * 안전 3종 위젯 게이트(2-3, 사업장축 2단 — SAFETY 활성식과 동형).
     * <ol>
     *   <li>역할 게이트: canManageCommon(master/safe) ∥ 현재 사업장 노드관리자.</li>
     *   <li>사업장 멤버십(IDOR): countUserSiteAuth(USE_YN='Y') &gt; 0.</li>
     * </ol>
     */
    private boolean resolveSafetyGate(String cmpnyCd, String userCd, String authCd,
            String effectiveSite, boolean nodeAdminInSite) {
        boolean companyWide = AuthRoleUtils.canManageCommon(authCd); // master ∥ safe
        if (!companyWide && !nodeAdminInSite) {
            return false;
        }
        // 사업장 멤버십 재검증(전사역할도 검증 — IDOR).
        return mapper.countUserSiteAuth(cmpnyCd, userCd, effectiveSite) > 0;
    }

    // ============================ 근태 카운트 빌드 ============================

    private DashboardSummaryResponse.Attendance buildAttendance(boolean available, String cmpnyCd,
            String authCd, String effectiveSite, List<String> scopedNodeCds) {
        if (!available) {
            return DashboardSummaryResponse.Attendance.builder().available(false).build();
        }
        // master/hr 는 사업장 전부서(노드 무필터), 그 외(노드관리자)는 노드 IN 필터.
        boolean companyWide = AuthRoleUtils.AUTH_MASTER.equals(authCd)
                || AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd);
        boolean useNodeScope = !companyWide;

        String todayYmd = mapper.selectTodayYmd();
        AttdCountQuery query = new AttdCountQuery(
                cmpnyCd, effectiveSite, todayYmd,
                useNodeScope, useNodeScope ? scopedNodeCds : Collections.emptyList());

        AttdCountResult cnt = mapper.selectAttdCounts(query);
        return DashboardSummaryResponse.Attendance.builder()
                .available(true)
                .checkedInCnt(cnt == null ? 0 : cnt.checkedInCnt())
                .scheduledCnt(cnt == null ? 0 : cnt.scheduledCnt())
                .leaveCnt(cnt == null ? 0 : cnt.leaveCnt())
                .build();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
