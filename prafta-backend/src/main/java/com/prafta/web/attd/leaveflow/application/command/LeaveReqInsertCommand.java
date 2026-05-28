package com.prafta.web.attd.leaveflow.application.command;

import java.math.BigDecimal;

/**
 * tb_user_attd_req(REQ_TYPE='05' 연차사용) INSERT 커맨드 (prafta-019-E).
 */
public record LeaveReqInsertCommand(
      String reqId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String reqStatus
    , String reqReason
    , String workYmd
    , String nodeCd
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , String leaveType
    , BigDecimal leaveDays
    , String insertNo
) {
}
