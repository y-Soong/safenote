package com.prafta.common.cmm.login.dto.response;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record LoginResponse(
	String cmpnyCd
	, String userCd
	, String userId
	, String userNm
	, String authCd
	, String authLevel
	, String siteCd
	, String siteNo
	, String siteNm
	, String nodeCd
	, String nodeNm
	, String refreshToken
	, String token
) {
	// 정책 §11.1 / §11.5: 로그인 응답 페이로드에서 자격 증명(비밀번호 해시) 및 PII(휴대폰/이메일) 제거.
	// 필요한 화면은 인증된 별도 API(/webApi/user01/user-info-lists)로 조회한다.
	public static LoginResponse from(UserResult userResult, String refreshToken, String token) {

		if (userResult == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (refreshToken == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (token == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LoginResponse(
    		userResult.cmpnyCd()
    		, userResult.userCd()
            , userResult.userId()
            , userResult.userNm()
            , userResult.authCd()
            , userResult.authLevel()
            , userResult.siteCd()
            , userResult.siteNo()
            , userResult.siteNm()
            , userResult.nodeCd()
            , userResult.nodeNm()
            , refreshToken
            , token
        );
    }
}
