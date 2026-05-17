package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserLogoutCommand(
	String cmpnyCd
	, String userCd
	, String clientType
	, String deviceId	
) {
	public static UserLogoutCommand from(LogoutParam param) {
		
		if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new UserLogoutCommand(
    		param.cmpnyCd()
    		, param.userCd()
    		, param.clientType()
    		, param.deviceId()
        );
    }
}
