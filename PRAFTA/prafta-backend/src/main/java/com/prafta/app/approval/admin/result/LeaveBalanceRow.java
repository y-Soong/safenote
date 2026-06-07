package com.prafta.app.approval.admin.result;

import java.math.BigDecimal;

/**
 * 001-P2-B3: 연차 상세 잔여(부여/사용/잔여) 집계 1행.
 */
public record LeaveBalanceRow(
      BigDecimal granted
    , BigDecimal used
    , BigDecimal remain
) {
}
