package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserPwdUnlockCommand(
	String cmpnyCd
	, String userCd
) {
	public static UserPwdUnlockCommand from(UserResult result) {
		
		if (result == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new UserPwdUnlockCommand(
    		result.cmpnyCd()
    		, result.userCd()
        );
    }
}
