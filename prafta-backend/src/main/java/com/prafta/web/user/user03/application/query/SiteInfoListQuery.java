package com.prafta.web.user.user03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user03.application.param.SiteInfoListParam;

public record SiteInfoListQuery(
	String userCd
	, String gvCmpnyCd
) {
	public static SiteInfoListQuery from(SiteInfoListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SiteInfoListQuery(
			param.userCd()
			, param.gvCmpnyCd()
		); 
	}
}
