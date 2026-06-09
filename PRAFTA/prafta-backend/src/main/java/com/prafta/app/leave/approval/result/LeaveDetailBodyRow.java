package com.prafta.app.leave.approval.result;

import java.math.BigDecimal;

/**
 * 사용자연차결재-01 (A-2 상세): 연차 본문(연차종류/유급여부/단위/구간) 1행 — selectLeaveBody 포팅.
 *
 * <p>paidYn 은 tb_leave_type_mgmt.PAID_TYPE(SYS023) 을 유급('Y')/무급('N') 으로 정규화한 값이다(F-PAID).
 */
public record LeaveDetailBodyRow(
      String leaveCd
    , String leaveNm
    , String paidYn
    , String useUnitType
    , String unitNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String startDate
    , String startTime
    , String endDate
    , String endTime
) {
}
