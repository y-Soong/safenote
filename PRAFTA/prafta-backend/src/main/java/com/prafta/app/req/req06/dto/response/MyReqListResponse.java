package com.prafta.app.req.req06.dto.response;

import java.util.List;

/**
 * prafta-app-006: 본인 요청 목록 조회 응답.
 *
 * @param totalCount    필터 무관 본인 전체 건수 (REQ_TYPE IN ('01'~'06') AND DEL_YN='N')
 * @param filteredCount 필터 적용 후 총합
 * @param items         현재 페이지 (최대 20건)
 * @param hasMore       다음 페이지 존재 여부 (limit+1 행 조회 결과 기반)
 */
public record MyReqListResponse(
        int totalCount
        , int filteredCount
        , List<MyReqItemResponse> items
        , boolean hasMore
) {
}
