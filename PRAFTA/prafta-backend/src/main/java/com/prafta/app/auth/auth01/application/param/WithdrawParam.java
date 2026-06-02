package com.prafta.app.auth.auth01.application.param;

import com.prafta.app.auth.auth01.dto.request.WithdrawRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-07: 회원 탈퇴 Param.
 */
public record WithdrawParam(
      boolean confirmed
    , TokenInfo tokenInfo
) {
    public static WithdrawParam from(WithdrawRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new WithdrawParam(request.isConfirmed(), tokenInfo);
    }
}
