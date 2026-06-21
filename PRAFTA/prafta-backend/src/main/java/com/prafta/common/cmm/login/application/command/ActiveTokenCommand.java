package com.prafta.common.cmm.login.application.command;

import java.util.Objects;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record ActiveTokenCommand(
	String cmpnyCd
	, String userCd
	, String tokenId
	, String loginId          // prafta-057: 로그인 세션 패밀리 식별자(다른 환경 로그인 감지용)
	, String clientType
	, String refreshTokenHash
	, String expireDtime
) {
	public static ActiveTokenCommand from(UserResult userResult, String tokenId, String loginId, String clientType, String refreshTokenHash, String expireDtime) {

			if(userResult == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);
			if(tokenId == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);
			if(loginId == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);
			if(clientType == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);
			if(refreshTokenHash == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);
			if(expireDtime == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ActiveTokenCommand(
    		userResult.cmpnyCd()
            , userResult.userCd()
            , tokenId
            , loginId
            , clientType
            , refreshTokenHash
            , expireDtime
        );
    }
}
