package com.prafta.app.approval.admin.result;

/**
 * 001-P2-B2: 근태보정(01/02)·초과(03/04) 대기 요청 1행(web ReqInboxMapper.selectPendingRequests 포팅).
 */
public record PendingCorrOtRow(
      String reqId
    , String reqType
    , String userCd
    , String userNm
    , String siteCd
    , String nodeCd
    , String nodeNm
    , String workYmd
    , Integer workSeq
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , String reqReason
    , String reqDate
) {
}
