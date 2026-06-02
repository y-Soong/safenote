package com.prafta.common.cmm.baseinfo.application.command;

import com.prafta.common.cmm.baseinfo.application.param.UserPasswordParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserPasswordCommand(
	String cmpnyCd
	, String userCd
	, String userPw
) {
	public static UserPasswordCommand from(UserPasswordParam param, String userPwHash) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(userPwHash == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserPasswordCommand(
			param.cmpnyCd()
			, param.userCd()
			, userPwHash
		); 
	}
}
