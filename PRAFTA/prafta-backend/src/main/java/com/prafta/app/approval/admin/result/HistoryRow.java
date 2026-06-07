package com.prafta.app.approval.admin.result;

/**
 * 001-P2-B6: 승인 이력(처리완료 02/03/04) 1행. PROCESS_DATE DESC 고정 정렬.
 */
public record HistoryRow(
      String reqId
    , String reqType
    , String userCd
    , String requesterUserNm
    , String nodeNm
    , String workYmd
    , String reqDate
    , String processDate
    , String reqStatus
    , String processUserCd
    , String processUserNm
    , String rejectReason
) {
}
