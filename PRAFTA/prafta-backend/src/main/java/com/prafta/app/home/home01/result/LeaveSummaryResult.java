package com.prafta.app.home.home01.result;

import java.math.BigDecimal;

/**
 * prafta-app-001: 연차 요약 합산 결과
 * (tb_user_leave_grant STATUS='ACTIVE' AND EXPIRE_YN='N' 전체 합산).
 * <p>매핑(AppHome01Mapper.selectLeaveSummary):
 * <pre>
 *   SUM(GRANT_DAYS)             AS grantedDays
 *   SUM(GRANT_DAYS - USED_DAYS) AS remainingDays
 * </pre>
 * 부여 이력이 없으면 SUM 이 null → 서비스에서 0.0 으로 폴백.
 */
public record LeaveSummaryResult(
    BigDecimal grantedDays
    , BigDecimal remainingDays
) {
}
