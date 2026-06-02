package com.prafta.app.leave.leave01.result;

import java.math.BigDecimal;

/**
 * prafta-app-005: 그룹별(TOTAL/STATUTORY/NON_STATUTORY) 연차 집계 결과.
 * <p>활성집합: tb_user_leave_grant STATUS='ACTIVE' AND EXPIRE_YN='N' AND DEL_YN='N' (home01 정합).
 * <p>매핑(AppLeave01Mapper.selectGroupAgg):
 * <pre>
 *   SUM(GRANT_DAYS)      AS granted    (활성·그룹)
 *   SUM(USED_DAYS)       AS usedTotal  (활성·그룹) — 과거사용 + 미래예정 포함(내부값)
 *   SUM(LU.LEAVE_DAYS)   AS planned    (CONFIRMED 미도래분, usedTotal 의 부분집합)
 * </pre>
 * 부여 이력이 없으면 SUM 이 null → 서비스에서 0.0 으로 폴백.
 * used/remaining/usageRate 는 서비스에서 파생 산출한다(decisions D-공식/D-Q5).
 */
public record LeaveGroupAggResult(
    BigDecimal granted
    , BigDecimal usedTotal
    , BigDecimal planned
) {
}
