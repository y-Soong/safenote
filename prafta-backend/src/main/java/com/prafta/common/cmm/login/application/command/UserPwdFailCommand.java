package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserPwdFailCommand(
	String cmpnyCd
	, String userCd
	, int lockDurationMinutes
	, int pwdFailMaxCnt
) {
	public static UserPwdFailCommand from(UserResult userResult, int lockDurationMinutes, int pwdFailMaxCnt) {
		
		if (userResult == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserResult");
		
        return new UserPwdFailCommand(
    		userResult.cmpnyCd()
    		, userResult.userCd()
    		, lockDurationMinutes
    		, pwdFailMaxCnt
        );
    }
}
