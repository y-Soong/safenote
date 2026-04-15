package com.prafta.web.baim.baim06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.SiteNodeParam;

public record SiteNodeCountQuery(
	String siteCd
	, String nodeCd
	, String gvCmpnyCd
){
	public static SiteNodeCountQuery from(SiteNodeParam param) {
	
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeParam");
		
		return new SiteNodeCountQuery(
			param.siteCd()
			, param.nodeCd()
			, param.gvCmpnyCd()
		);
	}
}
