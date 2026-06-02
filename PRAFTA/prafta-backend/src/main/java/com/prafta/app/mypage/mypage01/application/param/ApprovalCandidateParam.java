package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.ApprovalCandidateRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-05: 결재자 후보 검색 Param.
 */
public record ApprovalCandidateParam(
      String userNm
    , String nodeCd
    , TokenInfo tokenInfo
) {
    public static ApprovalCandidateParam from(ApprovalCandidateRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String userNm = (request == null) ? null : request.getUserNm();
        String nodeCd = (request == null) ? null : request.getNodeCd();
        return new ApprovalCandidateParam(userNm, nodeCd, tokenInfo);
    }
}
