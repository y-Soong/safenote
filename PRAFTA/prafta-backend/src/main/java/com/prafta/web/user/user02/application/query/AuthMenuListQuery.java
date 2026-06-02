package com.prafta.web.user.user02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user02.application.param.AuthMenuListParam;

public record AuthMenuListQuery(
	String menuDNm
	, String menuMNm
	, String authCd
	, String useYn
	, String gvCmpnyCd
){
	public static AuthMenuListQuery from(AuthMenuListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new AuthMenuListQuery(
			param.menuDNm()
			, param.menuMNm()
			, param.authCd()
			, param.useYn()
			, param.gvCmpnyCd()
		); 
	}
}
