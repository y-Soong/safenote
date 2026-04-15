package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserSmsAuthNoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserSmsAuthNoParam (
	String cmpnyCd
    , String mblNo
    , String dupChkYn
) {
	public static UserSmsAuthNoParam from(UserSmsAuthNoRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserSmsAuthNoRequest");
		
		return new UserSmsAuthNoParam(
			request.getCmpnyCd()
			, request.getMblNo()
			, request.getDupChkYn()
		); 
	}
}
