package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserIdDupleCheckRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserIdDupleCheckParam (
	String cmpnyCd
	, String userId
){
	public static UserIdDupleCheckParam from(UserIdDupleCheckRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserIdDupleCheckRequest");
		
		return new UserIdDupleCheckParam (
			request.getCmpnyCd()
			, request.getUserId()
		);
	}
}
