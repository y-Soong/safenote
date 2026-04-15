package com.prafta.web.baim.baim01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.dto.request.SiteInfoListRequest;

public record SiteInfoListParam(
	String cmpnyCd
	, String siteCd
	, String siteNo
	, String siteNm
	, String useYn
	, String gvCmpnyCd
){
	public static SiteInfoListParam from(SiteInfoListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteInfoListRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
		
		return new SiteInfoListParam(
			request.getCmpnyCd()
			, request.getSiteCd()
			, request.getSiteNo()
			, request.getSiteNm()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
		); 
	}
}
