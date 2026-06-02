package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserIdInfoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserIdInfoParam(
	String userNm
	, String mblNo
) {
	public static UserIdInfoParam from(UserIdInfoRequest request) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserIdInfoParam(
			request.getUserNm()
			, request.getMblNo()
		); 
	}
}
