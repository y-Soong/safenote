package com.prafta.app.leave.leaveflow.dto.response;

import java.util.List;

/**
 * prafta-app-018-A: 결재자 검색 응답 (GET /appApi/leaveflow/approver-search).
 *
 * <p>{@code hasNext} 는 size+1 조회로 다음 페이지 존재 판정. PII 최소노출(휴대폰/이메일/생년 미포함).
 *   키명은 018-C(FE)가 그대로 소비하므로 임의 변경 금지.
 */
public record ApproverSearchResponse(
      List<ApproverItem> approvers
    , boolean hasNext
) {
    /** 결재자 후보 1건. */
    public record ApproverItem(
          String userCd
        , String userId
        , String userNm
        , String rankNm
        , String nodeNm
    ) {
    }
}
