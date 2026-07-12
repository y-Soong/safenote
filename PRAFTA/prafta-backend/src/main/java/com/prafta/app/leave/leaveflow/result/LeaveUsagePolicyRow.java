package com.prafta.app.leave.leaveflow.result;

/**
 * prafta-app-018-A: 회사 활성 연차정책의 단일 허용단위 + 법정 결재여부.
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 *   {@code AppLeaveFlowMapper.selectCompanyUsageUnit} 의 SELECT 절과 1:1.
 * <ul>
 *   <li>{@code usageUnit} : tb_leave_usage_policy.USAGE_UNIT (FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30).</li>
 *   <li>{@code policyAprvUseYn} : tb_leave_policy.APRV_USE_YN (법정 결재여부).</li>
 *   <li>{@code allowQuarter} : tb_leave_usage_policy.ALLOW_QUARTER (반반차 0.25일 허용 토글 — LC-06).
 *       USAGE_UNIT 계층과 독립인 회사 단위 토글('Y'일 때만 SYS025 '05' 신청 허용).</li>
 * </ul>
 * 회사 활성 법정정책이 없으면 매퍼가 null 을 반환 → 서비스에서 FULL_DAY/'N' 폴백.
 */
public record LeaveUsagePolicyRow(
      String usageUnit
    , String policyAprvUseYn
    , String allowQuarter
) {
}
