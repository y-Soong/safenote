package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.WebMenuListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record WebMenuListParam(
	String cmpnyCd
	, String userCd
) {
	public static WebMenuListParam from(WebMenuListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - WebMenuListRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_003,"\n필수값 누락 - TokenInfo");
		
		return new WebMenuListParam(
			tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
