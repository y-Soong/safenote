package com.prafta.web.user.user02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user02.dto.request.AuthMenuListRequest;

public record AuthMenuListParam(
	String menuDNm
	, String menuMNm
	, String authCd
	, String useYn
	, String gvCmpnyCd
){
	public static AuthMenuListParam from(AuthMenuListRequest request, TokenInfo tokenInfo) {
		
		if(request == null) 
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - AuthMenuListRequest");
		if(tokenInfo == null) 
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
		
		return new AuthMenuListParam(
			request.getMenuDNm()
			, request.getMenuMNm()
			, request.getAuthCd()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}
