package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserIdInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserIdInfoQuery(
	String userNm
	, String mblNoHmac
) {
	public static UserIdInfoQuery from(UserIdInfoParam param, String mblNoHmac) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserIdInfoQuery(
			param.userNm()
			, mblNoHmac
		); 
	}
}
