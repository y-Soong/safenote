package com.prafta.web.attd.leaveflow.vo;

import java.math.BigDecimal;

/**
 * 차감 대상 부여(grant) 조회 결과 (prafta-019-E). 잔여 = GRANT_DAYS - USED_DAYS.
 */
public record DeductibleGrantVO(
      String grantId
    , BigDecimal grantDays
    , BigDecimal usedDays
) {
}
