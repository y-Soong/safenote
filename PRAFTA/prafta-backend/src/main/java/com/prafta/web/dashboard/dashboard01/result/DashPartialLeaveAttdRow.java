package com.prafta.web.dashboard.dashboard01.result;

/**
 * NF-2b(2026-08-07): 대시보드 A2 에서 <b>확정 부분연차(반차)가 있는 계획일</b>의 근태 원시행(차수 단위).
 *
 * <p>집계 쿼리({@code selectDashAttdStatusCount})는 이 집합을 {@code NOT EXISTS} 로 제외하고,
 * 서비스가 {@code PartialLeaveWindowUtils} 로 재판정해 카운트에 더한다. 두 쿼리가 같은 CTE·같은
 * EXISTS 조각을 쓰므로 두 집합은 정확히 상보 관계다(이중 계상·누락 불가).
 *
 * <p>근태행이 없는 계획일도 1행으로 온다({@code workSeq}/출퇴근이 모두 {@code null} → 미출근).
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code Dashboard01Mapper.selectDashPartialLeaveAttdRows} 와 순서 일치.
 */
public record DashPartialLeaveAttdRow(
        String userCd
        , String workYmd          // YYYYMMDD
        , Integer workSeq         // 1 | 2, 근태행 없으면 null
        , String planStart        // HHmm, nullable — 그 차수의 원 스케줄 시작
        , String planEnd          // HHmm, nullable — 그 차수의 원 스케줄 종료
        , String checkInDate      // YYYYMMDD, nullable
        , String checkInTime      // HHmm, nullable
        , String checkOutDate     // YYYYMMDD, nullable
        , String checkOutTime     // HHmm, nullable
) {
}
