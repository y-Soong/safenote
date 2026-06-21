package com.prafta.app.approval.admin.result;

import java.math.BigDecimal;

/**
 * 001-P2-B3: 상세(A-2) 메타 + 스코프/게이트 산출용 요청 1행.
 *
 * <p>requesterNodeCd 는 요청자 TB_USER.NODE_CD(요청행 NODE_CD 가 NULL 일 때 노드 스코프 fallback 용, A4).
 */
public record ReqMetaRow(
      String reqId
    , String reqType
    , String reqStatus
    , String userCd
    , String requesterUserNm
    , String siteCd
    , String nodeCd
    , String requesterNodeCd
    , String nodeNm
    , String workYmd
    , Integer workSeq
    , String targetId
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , String leaveType
    , BigDecimal leaveDays
    , String schCd
    , String reqReason
    , String reqDate
) {
}
