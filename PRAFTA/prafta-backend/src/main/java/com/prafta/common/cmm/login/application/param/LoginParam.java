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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(clientType == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new LoginParam(
            request.getUserId()
            , request.getUserPw()
            , clientType
        );
    }
}
