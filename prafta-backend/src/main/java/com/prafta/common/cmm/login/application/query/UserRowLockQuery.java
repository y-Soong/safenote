package com.prafta.common.cmm.login.application.query;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserRowLockQuery(
	String cmpnyCd
	, String userCd	
) {
	public static UserRowLockQuery from(UserResult userResult) {

		if (userResult == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserResult");
		
		return new UserRowLockQuery(
			userResult.cmpnyCd()
			, userResult.userCd()
        );
	}
}
