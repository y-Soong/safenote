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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserIdDupleCheckParam (
			request.getCmpnyCd()
			, request.getUserId()
		);
	}
}
