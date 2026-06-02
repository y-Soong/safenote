package com.prafta.common.cmm.login.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserTermsAgreementCheckParam(
	String userCd
){
    public static UserTermsAgreementCheckParam from(TokenInfo tokenInfo) {
    	
    	if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
    	
    	return new UserTermsAgreementCheckParam(tokenInfo.gv_userCd());    	
    }
}
