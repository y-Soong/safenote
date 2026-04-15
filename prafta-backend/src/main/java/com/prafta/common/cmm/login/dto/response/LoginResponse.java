package com.prafta.common.cmm.login.dto.response;

import java.util.Objects;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record LoginResponse(
	String cmpnyCd
	, String userCd
	, String userId
	, String userNm
	, String userPw
	, String authCd
	, String authLevel
	, String siteCd
	, String siteNo
	, String siteNm
	, String nodeCd
	, String nodeNm
	, String mblNo
	, String email
	, String refreshToken
	, String token
) {
	public static LoginResponse from(UserResult userResult, String mblNo, String email, String refreshToken, String token) {
		
		if (userResult == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserResult");
		if (mblNo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNo");
		if (email == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - email");
		if (refreshToken == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - refreshToken");
		if (token == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - token");
		
        return new LoginResponse(
    		userResult.cmpnyCd()
    		, userResult.userCd()
            , userResult.userId()
            , userResult.userNm()
            , userResult.userPw()
            , userResult.authCd()
            , userResult.authLevel()
            , userResult.siteCd()
            , userResult.siteNo()
            , userResult.siteNm()
            , userResult.nodeCd()
            , userResult.nodeNm()
            , mblNo
            , email
            , refreshToken
            , token
        );
    }
}
