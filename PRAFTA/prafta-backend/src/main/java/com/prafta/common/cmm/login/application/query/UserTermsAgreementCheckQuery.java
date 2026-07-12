package com.prafta.common.cmm.login.application.query;

import java.util.Objects;

import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;

public record UserTermsAgreementCheckQuery(
	String cmpnyCd
	, String userCd
){
	public static UserTermsAgreementCheckQuery from(UserTermsAgreementCheckParam param) {
    	Objects.requireNonNull(param, "UserTermsAgreementCheckParam is required");

    	// cmpnyCd 는 Param(=JWT)에서 전달된 서버 신뢰값. 회사 스코프 약관 동의 판정용.
    	return new UserTermsAgreementCheckQuery(param.cmpnyCd(), param.userCd());
    }
}
