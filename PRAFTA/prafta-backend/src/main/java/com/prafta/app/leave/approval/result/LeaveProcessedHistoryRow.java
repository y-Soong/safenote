package com.prafta.app.leave.approval.result;

import java.math.BigDecimal;

/**
 * 사용자연차결재-01 (A-5 이력, 신규 쿼리): 내가 처리(승인/반려)한 연차 요청 1행.
 *
 * <p>관리자 selectHistoryLeave(결재선 "참여" 기준)와 달리 "내 단계 행동 기준"이다(F-H1):
 * {@code AP.APPROVER_USER_CD = 토큰 userCd AND AP.APPROVAL_STATUS IN ('02','03')}.
 * myDecision = 내 단계 상태('02'승인/'03'반려), myComment = 내 단계 코멘트, myProcessDate = 내 단계 처리일시.
 */
public record LeaveProcessedHistoryRow(
      String reqId
    , Integer approvalStep
    , String reqType
    , String requesterUserNm
    , String nodeNm
    , String workYmd
    , String leaveNm
    , String unitNm
    , BigDecimal leaveDays
    , String reqDate
    , String myDecision
    , String myProcessDate
    , String myComment
    , String reqStatus
) {
}
