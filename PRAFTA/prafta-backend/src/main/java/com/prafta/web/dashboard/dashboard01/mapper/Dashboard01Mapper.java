package com.prafta.web.dashboard.dashboard01.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.dashboard.dashboard01.result.DashAcctGradeCountResult;
import com.prafta.web.dashboard.dashboard01.result.DashAttdPlanRegRateRowResult;
import com.prafta.web.dashboard.dashboard01.result.DashAttdStatusCountResult;
import com.prafta.web.dashboard.dashboard01.result.DashHalfLeaveWindowRow;
import com.prafta.web.dashboard.dashboard01.result.DashPartialLeaveAttdRow;
import com.prafta.web.dashboard.dashboard01.result.DashRecentAcctResult;
import com.prafta.web.dashboard.dashboard01.result.DashSiteBaselineResult;
import com.prafta.web.dashboard.dashboard01.result.LeaveUseSplitResult;
import com.prafta.web.dashboard.dashboard01.result.OvertimeMonthlyResult;
import com.prafta.web.dashboard.dashboard01.result.PatrolTodayResult;
import com.prafta.web.dashboard.dashboard01.result.RiskStatusCountResult;
import com.prafta.web.dashboard.dashboard01.result.TbmMonthCntResult;

/**
 * 웹 관리자 대시보드 매퍼 (PRAFTA-DASHBOARD-T1 골격 / T4 사고 · T5 순회점검/위험성평가/TBM 쿼리 추가).
 */
@Mapper
public interface Dashboard01Mapper {

    // ── T4: 안전 탭 무사고 배너(S1) + 사고 summary(S5) ──────────────

    // 사업장 접근 권한 확인 (tb_user_site_auth, USE_YN='Y'). 1 이상이면 접근 가능 — 모듈-로컬 중복 관례
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // 사업장 최근 사고 발생일 (없으면 null) — 무사고 배너 기산일
    String selectDashLatestAcctYmd(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
    );

    // 사고 이력 없는 사업장 기산점 (사업개시일/등록일) — 사이트 미존재 시 null
    DashSiteBaselineResult selectDashSiteBaseline(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
    );

    // 조회월(fromYmd~toYmd, YYYYMMDD) 사고 등급별 카운트 (조건부 집계 단건 — 0건 등급도 0 반환)
    DashAcctGradeCountResult selectDashAcctGradeCounts(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("fromYmd") String fromYmd
        , @Param("toYmd") String toYmd
    );

    // 전체 기간 최근 사고 3건 (PII 미포함 컬럼만)
    List<DashRecentAcctResult> selectDashRecentAcctList(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
    );

    // ── T5: 안전 탭 순회점검(S2) / 위험성평가(S3) / TBM 추이(S4) ─────

    // 당일 순회점검 이행 현황 (x=오늘 기록 보유 개소 / y=사용중 개소, 조건부 집계 단건)
    PatrolTodayResult selectPatrolToday(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("todayYmd") String todayYmd
    );

    // 조회월(fromYmd~toYmd, YYYYMMDD) 순회점검 미이행 총 횟수 (재귀 CTE 날짜 시리즈 × 개소, 기록 없는 날 카운트)
    int selectPatrolMonthMiss(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("fromYmd") String fromYmd
        , @Param("toYmd") String toYmd
    );

    // 위험성평가 상태별 카운트 (SYS011 001/002, 월 조건 없음 — Risk_03 목록 건수 일치 축)
    RiskStatusCountResult selectRiskStatusCounts(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
    );

    // 조회월(fromDate~toDate, YYYY-MM-DD) 아차사고 등록 건수 (OCCUR_DTIME 기준)
    int selectNearMissMonthCount(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("fromDate") String fromDate
        , @Param("toDate") String toDate
    );

    // 기간 내 TBM 완료 세션 월별 건수 (희소 — 건수 있는 월만 반환, 12포인트 0채움은 service)
    List<TbmMonthCntResult> selectTbmMonthlyCounts(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("fromDate") String fromDate
        , @Param("toDate") String toDate
    );

    // ── T2: 근태 탭 A1 근무계획 등록율 + A2 정상/비정상 근무율 ──────────

    // A1 부서별 근무계획 등록 카운트 (사용자 모수 = Attd_05 selectUserList 술어 미러)
    List<DashAttdPlanRegRateRowResult> selectDashAttdPlanRegRate(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
        , @Param("incSubNodeYn") String incSubNodeYn
        , @Param("workYm") String workYm
    );

    // A2 정상/지각/조퇴/미출근 일수 카운트 (일 단위 롤업 — Attd08 판정식 + Attd11 미출근 모수 이식)
    //   ★ NF-2b: 확정 부분연차(반차) 보유일은 이 집계에서 빠진다(서비스가 재판정해 더한다).
    DashAttdStatusCountResult selectDashAttdStatusCount(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
        , @Param("incSubNodeYn") String incSubNodeYn
        , @Param("workYm") String workYm
    );

    /**
     * NF-2b(2026-08-07): 확정 부분연차(반차) 보유 계획일의 근태 원시행(차수 단위).
     *
     * <p>{@code selectDashAttdStatusCount} 가 제외한 바로 그 집합이다(같은 CTE·같은 EXISTS 조각).
     * 지각·조퇴 판정은 서비스가 {@code PartialLeaveWindowUtils} 단일 출처로 수행한다 —
     * SQL 문자열 비교로는 야간 반차의 익일 경계를 표현할 수 없기 때문이다.
     */
    List<DashPartialLeaveAttdRow> selectDashPartialLeaveAttdRows(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
        , @Param("incSubNodeYn") String incSubNodeYn
        , @Param("workYm") String workYm
    );

    /** NF-2b: 그 달 확정 부분연차(반차) 면제 시각 구간 — 위 원시행 재판정의 입력. */
    List<DashHalfLeaveWindowRow> selectDashPartialLeaveWindows(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("workYm") String workYm
    );

    // ── T3: 근태 탭 A3 초과근무 6개월 추이(overtime-trend) / A4 법정연차 3분할(leave-usage) ─────

    // 기간(fromYmd~toYmd, YYYYMMDD) 초과근무 월별 SUM(WORK_MINUTES) (희소 — 존재 월만, 6포인트 0채움은 service).
    // 술어는 Attd_07/08 selectMonthlyOvertimeList 와 자구 동일 (DEL_YN='N' AND OT_STATUS <> 'CANCELLED' + node_tree INNER JOIN)
    List<OvertimeMonthlyResult> selectOvertimeMonthlyTotals(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
        , @Param("incSubNodeYn") String incSubNodeYn
        , @Param("fromYmd") String fromYmd
        , @Param("toYmd") String toYmd
    );

    // A4 법정 부여 합계(분모) — STATUTORY_% AND STATUS IN ('ACTIVE','EXHAUSTED')
    // (User01Mapper.selectUserStatutoryLeaveSummary + LeaveDashboard dashboardWhere 술어 조합)
    BigDecimal selectLeaveGrantTotal(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
        , @Param("incSubNodeYn") String incSubNodeYn
    );

    // A4 법정 사용/사용예정 분리 — GRANT_ID 경유 + 오늘 기준 CASE (LeaveDashboardMapper legalScheduled 관례)
    LeaveUseSplitResult selectLeaveUseSplit(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
        , @Param("incSubNodeYn") String incSubNodeYn
    );
}
