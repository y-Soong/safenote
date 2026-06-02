package com.prafta.web.baim.baim06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.SiteNodeListParam;

public record SiteNodeListQuery(
		String siteCd
		, String gvCmpnyCd
){
	public static SiteNodeListQuery from(SiteNodeListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SiteNodeListQuery(
			param.siteCd()
			, param.gvCmpnyCd()
		);
	}
}
