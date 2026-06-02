package com.prafta.common.cmm.login.application.param;

import com.prafta.common.cmm.login.dto.request.VerifyPhoneAuthRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.login.LoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;

/**
 * PRAFTA-036 — 휴대폰 본인인증 후 활성화 파라미터.
 *
 * <p>cmpnyCd / userCd 는 Authorization 임시 토큰의 claim 에서만 가져온다(IDOR 방지).
 * 임시 토큰의 scope 가 PHONE_AUTH 가 아니면 거부한다.
 */
public record VerifyPhoneAuthParam(
    String mblNo
    , String certNo
    , String gvCmpnyCd
    , String gvUserCd
    , String clientType
) {
    public static VerifyPhoneAuthParam from(VerifyPhoneAuthRequest request, TokenInfo tokenInfo, String scope, String clientType) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(LoginErrorCode.LOGIN_400_012);

        // 임시 토큰의 scope 가 PHONE_AUTH 가 아니면 거부 (PRAFTA-037-F8 상수화).
        if (!JwtScope.PHONE_AUTH.equals(scope))
            throw new ApiException(LoginErrorCode.LOGIN_400_012);

        return new VerifyPhoneAuthParam(
            request.getMblNo()
            , request.getCertNo()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , clientType
        );
    }
}
