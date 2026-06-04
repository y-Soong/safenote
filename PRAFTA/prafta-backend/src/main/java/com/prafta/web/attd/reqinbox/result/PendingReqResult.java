package com.prafta.web.attd.reqinbox.result;

/**
 * 요청 승인 관리 — 매니저 스코프 내 대기('01' 신청) 요청 1건 (prafta-019 후속).
 *
 * <p>근태 보정(REQ_TYPE 01/02)·초과근무(03) 탭 접수함 + 반려 키 운반.
 * 반려는 기존 /attd07/reject-user-* 엔드포인트가 키필드 일치를 요구하므로 그대로 노출한다.
 */
public record PendingReqResult(
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
