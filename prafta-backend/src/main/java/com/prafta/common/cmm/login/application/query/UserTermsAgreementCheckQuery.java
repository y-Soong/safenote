package com.prafta.common.cmm.login.application.query;

import java.util.Objects;

import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;

public record UserTermsAgreementCheckQuery(
	String userCd
){
	public static UserTermsAgreementCheckQuery from(UserTermsAgreementCheckParam param) {
    	Objects.requireNonNull(param, "UserTermsAgreementCheckParam is required");
    	
    	return new UserTermsAgreementCheckQuery(param.userCd());    	
    }
}
