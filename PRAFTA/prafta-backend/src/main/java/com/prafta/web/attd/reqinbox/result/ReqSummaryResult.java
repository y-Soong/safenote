package com.prafta.web.attd.reqinbox.result;

/**
 * 요청 1건 요약 (근태결재선통합 P3-1).
 *
 * <p>{@code GET /reqinbox/approval-line} 의 IDOR 검증(소속 사업장 접근 가능 여부 / 연차 요청자 본인
 * 여부)에 필요한 최소 컬럼만 담는다 — 목록 조회용 상세 컬럼은 포함하지 않는다.
 */
public record ReqSummaryResult(
      String reqId
    , String siteCd
    , String reqType
    , String userCd
) {
}
