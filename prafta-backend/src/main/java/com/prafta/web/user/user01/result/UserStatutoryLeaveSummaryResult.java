package com.prafta.web.user.user01.result;

import java.math.BigDecimal;

/**
 * 활성 법정 연차 부여 집계 결과 (prafta-022 작업 F, 입사일 변경 영향분석 실측용).
 *
 * <p>대상 = {@code tb_user_leave_grant} 의 활성 법정 부여 행:
 * {@code GRANT_TYPE LIKE 'STATUTORY\_%'} AND {@code STATUS IN ('ACTIVE','EXHAUSTED')}
 * AND {@code DEL_YN='N'} (정책서 §8.5.6 영향 스냅샷 / §8.5.8 기부여 보호).
 *
 * <p>집계 컬럼은 {@code COALESCE(SUM(...),0)} 으로 NULL이 0으로 정규화되어 반환된다.
 */
public record UserStatutoryLeaveSummaryResult(
    BigDecimal grantedSum   // 활성 법정부여 GRANT_DAYS 합계 (NULL→0)
    , BigDecimal usedSum     // 활성 법정부여 USED_DAYS 합계 (NULL→0)
) {
}
