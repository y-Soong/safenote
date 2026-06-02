package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 근태/연차 정보 조회 파라미터 (PRAFTA-017-4).
 * 대상 userCd는 path variable, 회사 스코프/권한은 토큰에서만 가져온다.
 */
public record LeaveInfoParam(
    String userCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static LeaveInfoParam from(String userCd, TokenInfo tokenInfo) {

        if (userCd == null || userCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new LeaveInfoParam(
            userCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
