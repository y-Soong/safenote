package com.prafta.common.cmm.login.application.param;

import com.prafta.common.cmm.login.dto.request.LoginRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record LoginParam(
	String userId
	, String userPw
	, String clientType
) {
	public static LoginParam from(LoginRequest request, String clientType) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - LoginRequest");
		if(clientType == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - clientType");
		
        return new LoginParam(
            request.getUserId()
            , request.getUserPw()
            , clientType
        );
    }
}
