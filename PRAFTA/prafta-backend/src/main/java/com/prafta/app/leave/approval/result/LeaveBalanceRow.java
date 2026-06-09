package com.prafta.app.leave.approval.result;

import java.math.BigDecimal;

/**
 * 사용자연차결재-01 (A-2 상세): 연차 잔여(부여/사용/잔여) 집계 1행 — selectLeaveBalance 포팅.
 */
public record LeaveBalanceRow(
      BigDecimal granted
    , BigDecimal used
    , BigDecimal remain
) {
}
