package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.result.RequiredTermsResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record RequiredTermsInfoCommand(
	String cmpnyCd
	, String userCd
    , String termsId
    , String termsVersion
    , String agrYn
){
	public static RequiredTermsInfoCommand from(String userCd, UserJoinParam param, RequiredTermsResult result) {

		if(userCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(result == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 약관 동의기록은 회사 스코프. 신규 생성되는 TB_USER 와 동일한 회사(param.cmpnyCd())로 적재한다.
		return new RequiredTermsInfoCommand(
			param.cmpnyCd()
			, userCd
			, result.termsId()
			, result.termsVersion()
			, "Y"
        );
    }
}
