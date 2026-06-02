package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserPasswdRequest;

public record UserPasswdParam(
	String cmpnyCd
	, String userCd
){
	public static UserPasswdParam from(UserPasswdRequest request, TokenInfo tokenInfo) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 유효 로그인 토큰 필수화: 토큰 미존재/무효 시 거부한다.
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		// 회사 식별자는 request 값을 무시하고 토큰 값으로 강제한다 (회사 경계 위조 방지).
		return new UserPasswdParam(
			tokenInfo.gv_cmpnyCd()
			, request.getUserCd()
		);
	}
}
