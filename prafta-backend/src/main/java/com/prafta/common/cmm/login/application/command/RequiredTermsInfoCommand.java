package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.result.RequiredTermsResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record RequiredTermsInfoCommand(
	String userCd
    , String termsId
    , String termsVersion
    , String agrYn
){
	public static RequiredTermsInfoCommand from(String userCd, UserJoinParam param, RequiredTermsResult result) {
		
		if(userCd == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - userCd");
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserJoinParam");
		if(result == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RequiredTermsResult");
		
		return new RequiredTermsInfoCommand(
			userCd
			, result.termsId()
			, result.termsVersion()
			, "Y"
        );
    }
}
