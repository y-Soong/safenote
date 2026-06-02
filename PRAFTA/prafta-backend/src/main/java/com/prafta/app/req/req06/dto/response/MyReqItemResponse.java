package com.prafta.app.req.req06.dto.response;

import java.util.List;

/**
 * prafta-app-006: 응답의 개별 요청 행.
 *
 * <p>플랫 구조 — 프론트 카드 컴포넌트가 그대로 사용.
 *
 * @param summaryLines REQ_TYPE 별로 백엔드가 가공한 요약 줄 (1~2줄). 빈 리스트 가능.
 * @param rejectReason REQ_STATUS='03' 일 때만 PROCESS_COMMENT, 그 외 null.
 */
public record MyReqItemResponse(
        String reqId
        , String reqType
        , String reqTypeDisplay
        , String reqStatus
        , String reqStatusDisplay
        , String targetYmd
        , String targetYmdDisplay
        , List<String> summaryLines
        , String reqDatetime
        , String reqDateDisplay
        , String processedAt
        , String processedDateDisplay
        , String rejectReason
) {
}
