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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserIdInfoRequest");
		
		return new UserIdInfoParam(
			request.getUserNm()
			, request.getMblNo()
		); 
	}
}
