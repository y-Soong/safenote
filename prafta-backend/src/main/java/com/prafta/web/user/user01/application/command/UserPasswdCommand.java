package com.prafta.web.user.user01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.UserPasswdParam;

public record UserPasswdCommand(
	String cmpnyCd
	, String userCd
	, String userPw
) {
	public static UserPasswdCommand from(UserPasswdParam param, String userPw) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(userPw == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserPasswdCommand(
			param.cmpnyCd()
			, param.userCd()
			, userPw
		);
	}
}
