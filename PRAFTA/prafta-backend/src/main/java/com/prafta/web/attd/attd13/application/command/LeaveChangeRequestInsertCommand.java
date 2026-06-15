package com.prafta.web.attd.attd13.application.command;

/**
 * 연차 변경/삭제 요청 INSERT 운반체 (PRAFTA-COM-008-C).
 *
 * <p>서비스에서 채번한 {@code changeReqId} 와 검증을 통과한 값만 담는다.
 */
public record LeaveChangeRequestInsertCommand(
      String changeReqId
    , String cmpnyCd
    , String siteCd
    , String targetUserCd
    , String targetLeaveId
    , String initiatorType
    , String reqType
    , String moveTargetDate
    , String reqReason
    , String initiatorUserCd
    , String insertNo
) {
}
