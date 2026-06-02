package com.prafta.app.mypage.mypage01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-05: 결재자 후보 목록 응답.
 */
@Getter
@Builder
public class ApprovalCandidateListResponse {

    /** 본인 직급 SORT_IDX (없으면 null). FE 정렬 보조용. */
    private final Integer myRankSortIdx;
    private final List<ApprovalCandidateItem> candidates;
}
