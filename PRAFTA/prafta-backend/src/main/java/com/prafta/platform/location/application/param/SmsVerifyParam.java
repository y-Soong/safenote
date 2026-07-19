package com.prafta.platform.location.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.location.dto.request.SmsVerifyRequest;

/**
 * SMS 인증번호 검증 파라미터(sms-verify).
 *
 * <p>휴대폰번호는 받지 않는다 — 검증 대상은 토큰의 운영자 본인 등록 휴대폰(HMAC)으로만 매칭.
 */
public record SmsVerifyParam(
    String gvCmpnyCd
    , String gvUserCd
    , String certNo
) {
    public static SmsVerifyParam from(SmsVerifyRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new SmsVerifyParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , request.getCertNo()
        );
    }
}
