package com.prafta.common.cmm.dailylogin.application.command;

import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-app-027-2 — 일용직 리프레시 토큰 세션 명령(TB_AUTH_TOKEN). 정규 ActiveTokenCommand 미러.
 */
public record DailyActiveTokenCommand(
    String cmpnyCd
    , String userCd
    , String tokenId
    , String clientType
    , String refreshTokenHash
    , String expireDtime
) {
    public static DailyActiveTokenCommand from(DailyUserResult userResult, String tokenId, String clientType,
                                               String refreshTokenHash, String expireDtime) {

        if (userResult == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenId == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (clientType == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (refreshTokenHash == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (expireDtime == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new DailyActiveTokenCommand(
            userResult.cmpnyCd()
            , userResult.userCd()
            , tokenId
            , clientType
            , refreshTokenHash
            , expireDtime
        );
    }
}
