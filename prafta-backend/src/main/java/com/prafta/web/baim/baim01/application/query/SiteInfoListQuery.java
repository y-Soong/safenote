package com.prafta.web.baim.baim01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.param.SiteInfoListParam;

public record SiteInfoListQuery(
	String cmpnyCd
	, String siteCd
	, String siteNo
	, String siteNm
	, String useYn
	, String gvCmpnyCd
) {
	public static SiteInfoListQuery from(SiteInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SiteInfoListQuery(
			param.cmpnyCd()
			, param.siteCd()
			, param.siteNo()
			, param.siteNm()
			, param.useYn()
			, param.gvCmpnyCd()
		); 
	}
}
