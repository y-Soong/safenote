package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record WithdrawMyAccountParam(
    String cmpnyCd,
    String userCd
) {
    public static WithdrawMyAccountParam from(TokenInfo tokenInfo) {

        // 내 계정 탈퇴 대상은 오직 토큰에서만 결정한다 (IDOR 방지). 토큰 미존재/무효 시 거부.
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new WithdrawMyAccountParam(
            tokenInfo.gv_cmpnyCd(),
            tokenInfo.gv_userCd()
        );
    }
}
