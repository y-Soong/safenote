package com.prafta.web.attd.attd07.application.model;

/**
 * Validated and normalized single overtime segment carried through the service layer.
 * Field semantics match {@code TB_USER_OVERTIME_MGMT} columns.
 */
public record OvertimeItemModel(
      // com-013-06 A - 관리자 직접수정 in-place UPDATE 대상 식별자(기존 OT 행이면 보유, 신규행이면 null).
      String otId
    , String startDate
    , String startTime
    , String endDate
    , String endTime
) {
}
