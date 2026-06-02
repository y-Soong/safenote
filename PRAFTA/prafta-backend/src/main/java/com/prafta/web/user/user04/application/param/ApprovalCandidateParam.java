package com.prafta.web.user.user04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user04.dto.request.ApprovalCandidateRequest;

/**
 * 결재자 후보 조회 Param (prafta-019-D).
 * 스코프(회사/사업장)는 토큰 클레임으로 강제하여 cross-site/cross-tenant 누출을 차단한다.
 */
public record ApprovalCandidateParam(
      String userNm
    , String nodeCd
    , String gvCmpnyCd
    , String gvSiteCd
    , String gvUserCd
) {
    public static ApprovalCandidateParam from(ApprovalCandidateRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new ApprovalCandidateParam(
              request.getUserNm()
            , request.getNodeCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
        );
    }
}
