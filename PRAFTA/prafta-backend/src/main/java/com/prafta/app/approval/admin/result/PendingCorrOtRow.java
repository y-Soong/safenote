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
    , String schCd
    // PRAFTA-APP-029 후속: 스케줄 수정(10) 카드 요약용 — 요청 스케줄(REQ.SCH_CD)의 근무일 기준 유효버전 시각.
    //   비스케줄(근태보정/초과) 행은 SCH_CD 가 null 이라 조인 미매치 → 전부 null.
    , String schFstStrTime
    , String schFstEndTime
    , String schSecStrTime
    , String schSecEndTime
    , String reqReason
    , String reqDate
) {
}
