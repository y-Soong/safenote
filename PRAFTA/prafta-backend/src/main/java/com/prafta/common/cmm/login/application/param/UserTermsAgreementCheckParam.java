package com.prafta.common.cmm.login.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserTermsAgreementCheckParam(
	String cmpnyCd
	, String userCd
){
    public static UserTermsAgreementCheckParam from(TokenInfo tokenInfo) {

    	if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

    	// cmpnyCd/userCd 모두 JWT 클레임에서만 도출(회사 스코프 약관 동의 조회, IDOR 차단).
    	return new UserTermsAgreementCheckParam(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
    }
}
