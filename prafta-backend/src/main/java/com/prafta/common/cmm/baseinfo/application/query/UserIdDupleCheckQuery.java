package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserIdDupleCheckParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserIdDupleCheckQuery (
	String cmpnyCd
	, String userId
){
	public static UserIdDupleCheckQuery from(UserIdDupleCheckParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserIdDupleCheckQuery (
			param.cmpnyCd()
			, param.userId()
		);
	}
}
