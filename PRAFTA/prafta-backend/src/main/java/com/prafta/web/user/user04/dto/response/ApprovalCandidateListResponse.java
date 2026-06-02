package com.prafta.web.user.user04.dto.response;

import java.util.List;

import com.prafta.web.user.user04.result.ApprovalCandidateResult;

import lombok.Builder;
import lombok.Value;

/**
 * 결재자 후보 목록 응답 (prafta-019-D).
 *
 * <p>{@code myRankSortIdx}는 신청자 본인의 직급 순서로, 프리셋("내 위 직급 순") 임계값이다.
 * 직급 미배정이면 null.
 */
@Value
@Builder
public class ApprovalCandidateListResponse {

    Integer myRankSortIdx;
    List<ApprovalCandidateResult> candidates;
}
