package com.prafta.web.user.user01.application.param;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserPasswdRequest;

public record UserPasswdParam(
	String cmpnyCd
	, String userCd
){
	public static UserPasswdParam from(UserPasswdRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserPasswdRequest");
				
		return new UserPasswdParam(
			request.getCmpnyCd()
			, request.getUserCd()
		);
	}
}
