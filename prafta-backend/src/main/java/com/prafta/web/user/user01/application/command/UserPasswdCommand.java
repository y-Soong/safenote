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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserPasswdParam");
		if(userPw == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - userPw");
		
		return new UserPasswdCommand(
			param.cmpnyCd()
			, param.userCd()
			, userPw
		);
	}
}
