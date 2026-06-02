package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-05: 결재자 후보 1건 응답 항목.
 */
@Getter
@Builder
public class ApprovalCandidateItem {

    private final String userCd;
    private final String userNm;
    private final String rankNm;
    private final String nodeNm;
}
