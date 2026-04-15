package com.prafta.web.baim.baim06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.dto.request.SiteNodeListRequest;

public record SiteNodeListParam(
	String siteCd
	, String gvCmpnyCd
){
	public static SiteNodeListParam from(SiteNodeListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeListRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
		
		return new SiteNodeListParam(
			request.getSiteCd()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}
