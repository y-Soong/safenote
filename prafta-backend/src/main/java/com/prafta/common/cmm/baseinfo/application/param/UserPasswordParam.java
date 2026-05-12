package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.UserPasswordRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserPasswordParam (
	String cmpnyCd
	, String userCd
	, String userPw
){
public static UserPasswordParam from(UserPasswordRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserPasswordRequest");
		
        return new UserPasswordParam(
    		request.getCmpnyCd()
    		, request.getUserCd()
    		, request.getUserPw()
        );
    }
}
