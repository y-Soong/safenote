package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.SiteInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteInfoParam(
	String cmpnyCd
	, String userCd
	, String siteNo
	, String siteNm
) {
	public static SiteInfoParam from(SiteInfoRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteInfoRequest");
		
        return new SiteInfoParam(
    		request.getCmpnyCd()
    		, tokenInfo != null ? tokenInfo.gv_userCd() : ""
    		, request.getSiteNo()
    		, request.getSiteNm()
        );
    }
}
