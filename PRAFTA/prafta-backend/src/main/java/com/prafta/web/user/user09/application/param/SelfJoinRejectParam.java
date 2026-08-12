package com.prafta.web.user.user09.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user09.dto.request.SelfJoinRejectRequest;

/**
 * 소정-09: 셀프가입 거부 파라미터. 식별/권한은 JWT 클레임에서만 도출한다.
 */
public record SelfJoinRejectParam(
        String userCd
        , String rejectReason
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    public static SelfJoinRejectParam from(SelfJoinRejectRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getUserCd() == null || request.getUserCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SelfJoinRejectParam(
                request.getUserCd().trim()
                , request.getRejectReason() == null ? null : request.getRejectReason().trim()
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }
}
