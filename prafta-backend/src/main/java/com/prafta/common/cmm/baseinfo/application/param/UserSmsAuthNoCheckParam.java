package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserSmsAuthNoCheckRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserSmsAuthNoCheckParam(
	String cmpnyCd
    , String mblNo
    , String certNo
) {
	public static UserSmsAuthNoCheckParam from(UserSmsAuthNoCheckRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserSmsAuthNoCheckRequest");
		
		return new UserSmsAuthNoCheckParam(
			request.getCmpnyCd()
			, request.getMblNo()
			, request.getCertNo()
		); 
	}
}
