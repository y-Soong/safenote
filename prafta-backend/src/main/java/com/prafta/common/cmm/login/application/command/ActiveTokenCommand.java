package com.prafta.common.cmm.login.application.command;

import java.util.Objects;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record ActiveTokenCommand(
	String cmpnyCd
	, String userCd
	, String tokenId
	, String clientType
	, String refreshTokenHash
	, String expireDtime
) {
	public static ActiveTokenCommand from(UserResult userResult, String tokenId, String clientType, String refreshTokenHash, String expireDtime) {

			if(userResult == null)
				throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserResult");
			if(tokenId == null)
				throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - tokenId");
			if(clientType == null)
				throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - clientType");
			if(refreshTokenHash == null)
				throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - refreshTokenHash");
			if(expireDtime == null)
				throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - expireDtime");
		
        return new ActiveTokenCommand(
    		userResult.cmpnyCd()
            , userResult.userCd()
            , tokenId
            , clientType
            , refreshTokenHash
            , expireDtime
        );
    }
}
