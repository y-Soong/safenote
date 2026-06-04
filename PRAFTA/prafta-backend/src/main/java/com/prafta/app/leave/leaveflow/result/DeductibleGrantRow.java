package com.prafta.app.leave.leaveflow.result;

import java.math.BigDecimal;

/**
 * prafta-app-018-B: 차감 대상 부여 1행(만료 임박 우선, FOR UPDATE).
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.vo.DeductibleGrantVO} 미러.
 * ⚠️ MyBatis record 위치매핑 — SELECT 컬럼 순서(grantId, grantDays, usedDays)와 일치해야 한다.
 */
public record DeductibleGrantRow(
      String grantId
    , BigDecimal grantDays
    , BigDecimal usedDays
) {
}
