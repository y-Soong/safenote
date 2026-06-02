package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.MobileVerifyRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-03b: 휴대폰 변경 인증 검증 Param.
 */
public record MobileVerifyParam(
      String mblNo
    , String verificationCode
    , TokenInfo tokenInfo
) {
    public static MobileVerifyParam from(MobileVerifyRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new MobileVerifyParam(request.getMblNo(), request.getVerificationCode(), tokenInfo);
    }
}
