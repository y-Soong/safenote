package com.prafta.app.approval.admin.result;

import java.math.BigDecimal;

/**
 * 001-P2-B3: 연차 상세 본문(연차종류/단위/구간) 1행.
 */
public record LeaveBodyRow(
      String leaveCd
    , String leaveNo
    , String leaveNm
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
