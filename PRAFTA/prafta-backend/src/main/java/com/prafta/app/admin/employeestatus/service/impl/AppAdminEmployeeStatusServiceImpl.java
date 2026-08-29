package com.prafta.app.admin.employeestatus.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.app.admin.common.scope.application.helper.AdminAttdLikeScopeResolver;
import com.prafta.app.admin.employeestatus.application.param.EmployeeGpsTrailParam;
import com.prafta.app.admin.employeestatus.application.param.EmployeeStatusDailyParam;
import com.prafta.app.admin.employeestatus.application.query.EmployeeStatusScopeQuery;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeGpsTrailResponse;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeStatusDailyResponse;
import com.prafta.app.admin.employeestatus.mapper.AppAdminEmployeeStatusMapper;
import com.prafta.app.admin.employeestatus.result.EmployeeGpsTrailRow;
import com.prafta.app.admin.employeestatus.result.EmployeeOwnerScopeResult;
import com.prafta.app.admin.employeestatus.result.EmployeeStatusRosterRow;
import com.prafta.app.admin.employeestatus.service.AppAdminEmployeeStatusService;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.GpsCoordCrypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-002/003: 앱 관리자 "직원관리"(실시간 근태 현황 + 외근 위치) 서비스 구현(조회 전용).
 *
 * <p>PRAFTA-002 인가는 2단 게이트다 — ① {@link SiteAccessService#assertSiteAccess}(사업장 인가, 원장 기반)
 * ② {@link AdminAttdLikeScopeResolver}(부서 스코프, ATTD_DETAIL과 동일 축). <b>둘 다 서비스 계층에서
 * 강제한다</b>(컨트롤러가 아님 — 메모리 {@code feedback_web_new_query_screen_needs_node_gate} 3회 재발
 * 이력, 신규 조회화면은 이 함정을 다시 밟지 않는다). {@code nodeCd}/{@code siteCd} 파라미터는 그대로
 * 신뢰하지 않고 항상 서버가 재검증한다.
 *
 * <p>PRAFTA-003(GPS 궤적)은 웹 {@code Attd08ServiceImpl.getAttdGpsTrail}의 3단 게이트(① 소유 스코프 조회
 * ② 사업장 인가 ③ 부서 스코프)를 그대로 이식하되, ③은 {@code AttdCloseService}(web 전용 레이어) 대신
 * {@link AdminAttdLikeScopeResolver}를 공유한다(§0-2 근거 — 앱 패키지는 앱 자체 게이트 메커니즘을 쓴다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminEmployeeStatusServiceImpl implements AppAdminEmployeeStatusService {

    private final AppAdminEmployeeStatusMapper mapper;
    /** 사업장 접근 인가(User_03 원장 TB_USER_SITE_AUTH 기반) — PRAFTA-002/003 공용. */
    private final SiteAccessService siteAccessService;
    /** ATTD_DETAIL 축 부서 스코프 판정 — AppAdminAttdServiceImpl 과 공유(§PRAFTA-002 상세 권장). */
    private final AdminAttdLikeScopeResolver scopeResolver;
    /** GPS좌표 fallback 복호화(ENC 우선, NULL 이면 구 평문) — 공통 빈, 재사용. */
    private final GpsCoordCrypto gpsCoordCrypto;

    // 정책서 attd §14.2 — 상태 4종(SQL CASE 금지, Java 산출).
    private static final String STATUS_WORKING = "WORKING";
    private static final String STATUS_ABSENT = "ABSENT";
    private static final String STATUS_DAY_OFF = "DAY_OFF";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    // 로스터 전체 조회 후 메모리 슬라이스 상한(AppAdminAttdServiceImpl과 동일 관례 — DoS 방어용 넉넉한 천장).
    private static final int MAX_OFFSET = 10000;

    // ============================ PRAFTA-002: 일자 직원 현황 ============================

    @Override
    public EmployeeStatusDailyResponse selectDaily(EmployeeStatusDailyParam param) {

        // ① 사업장 인가 — 토큰 소속 밖 사업장을 siteCd 로 지정하는 시도를 원장 기반으로 차단.
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(),
                param.gvSiteCd(), param.siteCd());

        // ② 부서 스코프(ATTD_DETAIL 과 동일 축) — nodeCd 를 그대로 신뢰하지 않고 서버가 재검증.
        AdminAttdLikeScopeResolver.ScopeContext scope =
                scopeResolver.resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.siteCd(), param.gvAuthCd());
        List<String> nodes = scopeResolver.effectiveScopedNodes(scope, param.nodeCd());

        // IDOR: 노드관리자가 스코프 밖 노드를 지정 → 빈 결과(노출 0).
        if (nodes != null && nodes.isEmpty()) {
            return EmployeeStatusDailyResponse.builder()
                    .items(Collections.emptyList()).totalCount(0).hasMore(false).build();
        }

        boolean useNodeScope = nodes != null;
        EmployeeStatusScopeQuery query = new EmployeeStatusScopeQuery(
                param.gvCmpnyCd(), param.siteCd(),
                !useNodeScope, useNodeScope, useNodeScope ? nodes : Collections.emptyList(),
                param.keyword(), param.workYmd());

        List<EmployeeStatusRosterRow> rows = mapper.selectDailyRoster(query);

        List<EmployeeStatusDailyResponse.DailyItem> all = new ArrayList<>(rows.size());
        for (EmployeeStatusRosterRow r : rows) {
            all.add(toDailyItem(r));
        }

        int totalCount = all.size();
        int offset = (param.page() - 1) * param.pageSize();
        List<EmployeeStatusDailyResponse.DailyItem> items = slice(all, offset, param.pageSize());
        boolean hasMore = offset + items.size() < totalCount;

        log.info("앱 관리자 직원 현황 조회 - cmpnyCd={}, siteCd={}, workYmd={}, 대상={}명",
                param.gvCmpnyCd(), param.siteCd(), param.workYmd(), totalCount);

        return EmployeeStatusDailyResponse.builder()
                .items(items).totalCount(totalCount).hasMore(hasMore).build();
    }

    /**
     * 로스터 원시 행 → 응답 아이템. 상태 산출 순서(plan §PRAFTA-002 상세, SQL CASE 금지):
     * 근무계획 없음 → DAY_OFF, 근무계획 있음+출근없음 → ABSENT, 출근+퇴근없음 → WORKING, 둘다 있음 → CHECKED_OUT.
     * 연차(isOnLeave)는 상태와 배타가 아닌 additive 배지다(§2-3).
     */
    private EmployeeStatusDailyResponse.DailyItem toDailyItem(EmployeeStatusRosterRow r) {
        boolean hasWorkPlan = "Y".equals(r.hasWorkPlanYn());
        String checkInTime = blankToNull(r.checkInTime());
        String checkOutTime = blankToNull(r.checkOutTime());

        String status;
        if (!hasWorkPlan) {
            status = STATUS_DAY_OFF;
        } else if (checkInTime == null) {
            status = STATUS_ABSENT;
        } else if (checkOutTime == null) {
            status = STATUS_WORKING;
        } else {
            status = STATUS_CHECKED_OUT;
        }

        return EmployeeStatusDailyResponse.DailyItem.builder()
                .userCd(r.userCd())
                .userNm(r.userNm())
                .nodeNm(r.nodeNm())
                .status(status)
                .isOnLeave("Y".equals(r.isOnLeaveYn()))
                .isOffsite("Y".equals(r.isOffsiteYn()))
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .attdIds(splitAttdIds(r.attdIds()))
                .build();
    }

    private List<String> splitAttdIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(csv.split(","));
    }

    // ============================ PRAFTA-003: GPS 궤적 ============================

    @Override
    public EmployeeGpsTrailResponse selectGpsTrail(EmployeeGpsTrailParam param) {

        // ① 소유 스코프 조회 — attdId 만으로는 인가 판정이 불가하므로 그 근태 행의 사업장/부서를 먼저 읽는다.
        EmployeeOwnerScopeResult scope = mapper.selectAttdOwnerScope(param.gvCmpnyCd(), param.attdId());
        if (scope == null) {
            // 존재하지 않거나 타 회사 근태 — 존재 여부를 흘리지 않도록 권한 없음으로 수렴(웹과 동일 원칙).
            log.warn("앱 관리자 직원 GPS 궤적 조회 거부(대상 없음) - userCd={}, attdId={}",
                    param.gvUserCd(), param.attdId());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // ② 사업장 인가.
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(),
                param.gvSiteCd(), scope.siteCd());

        // ③ 부서 스코프 — PRAFTA-002 와 동일한 판정을 공유(AttdCloseService 미사용, §0-2 근거).
        AdminAttdLikeScopeResolver.ScopeContext ctx =
                scopeResolver.resolveScope(param.gvCmpnyCd(), param.gvUserCd(), scope.siteCd(), param.gvAuthCd());
        if (!scopeResolver.isNodeAllowed(ctx, scope.nodeCd())) {
            log.warn("앱 관리자 직원 GPS 궤적 조회 권한 없음 - userCd={}, authCd={}, attdId={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.attdId(), scope.siteCd(), scope.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        List<EmployeeGpsTrailRow> rows = mapper.selectAttdGpsTrail(param.gvCmpnyCd(), param.attdId());

        // 행 단위 fallback 복호화(ENC 우선, NULL 이면 구 평문) — 좌표 평문/복호화값은 로그에 남기지 않는다.
        List<EmployeeGpsTrailResponse.TrailItem> trail = new ArrayList<>(rows.size());
        for (EmployeeGpsTrailRow row : rows) {
            trail.add(EmployeeGpsTrailResponse.TrailItem.builder()
                    .gpsId(row.gpsId())
                    .lat(gpsCoordCrypto.resolveToBigDecimal(row.latEnc(), row.lat()))
                    .lon(gpsCoordCrypto.resolveToBigDecimal(row.lonEnc(), row.lon()))
                    .accuracy(row.accuracy())
                    .apiCallDate(row.apiCallDate())
                    .apiCallTime(row.apiCallTime())
                    .isMocked(row.isMocked())
                    .gpsInfoType(row.gpsInfoType())
                    .build());
        }

        log.info("앱 관리자 직원 GPS 궤적 조회 - attdId={}, 건수={}건", param.attdId(), trail.size());

        return EmployeeGpsTrailResponse.builder().trail(trail).build();
    }

    // ============================ 보조 ============================

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private <T> List<T> slice(List<T> all, int offset, int size) {
        if (offset > MAX_OFFSET || offset >= all.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(all.subList(offset, Math.min(offset + size, all.size())));
    }
}
