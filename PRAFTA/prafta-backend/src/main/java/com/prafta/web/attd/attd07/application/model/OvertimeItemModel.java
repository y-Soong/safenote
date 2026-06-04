package com.prafta.web.attd.attd07.application.model;

/**
 * Validated and normalized single overtime segment carried through the service layer.
 * Field semantics match {@code TB_USER_OVERTIME_MGMT} columns.
 */
public record OvertimeItemModel(
      String startDate
    , String startTime
    , String endDate
    , String endTime
) {
}
