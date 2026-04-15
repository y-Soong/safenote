package com.prafta.common.cmm.login.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserTermsAgreementCheckParam(
	String userCd
){
    public static UserTermsAgreementCheckParam from(TokenInfo tokenInfo) {
    	
    	if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
    	
    	return new UserTermsAgreementCheckParam(tokenInfo.gv_userCd());    	
    }
}
