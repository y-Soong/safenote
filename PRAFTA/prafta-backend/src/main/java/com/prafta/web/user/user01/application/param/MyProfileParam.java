package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record MyProfileParam(
	String gvCmpnyCd
	, String gvUserCd
){
	public static MyProfileParam from(TokenInfo tokenInfo) {

		// 조회 대상은 오직 토큰에서만 결정한다 (IDOR 방지). 토큰 미존재/무효 시 거부.
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new MyProfileParam(
			tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
