package com.prafta.app.approval.admin.result;

import java.math.BigDecimal;

/**
 * 001-P2-B2: 연차(05/06) 대기 요청 1행(web LeaveFlowMapper.selectMyPendingLeaveApprovals 포팅).
 *
 * <p>연차는 결재선(APPROVER_USER_CD) 기반 — "내가 현재 단계 결재자"인 요청만 노출(web 동일).
 */
public record PendingLeaveRow(
      String reqId
    , Integer approvalStep
    , String reqType
    , String requesterUserCd
    , String requesterUserNm
    , String nodeNm
    , String workYmd
    , String leaveType
    , String leaveCd
    , String leaveNo
    , String leaveNm
    , String useUnitType
    , String unitNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String startTime
    , String endTime
    , String reqReason
    , String reqDate
    , String selfYn
) {
}
