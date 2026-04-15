package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.SiteNodeListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteNodeListQuery(
	String cmpnyCd
	, String userCd
	, String siteCd
	, String nodeCd
	, String nodeType
	, String nodeNm
	, String parentNodeNm
) {
	public static SiteNodeListQuery from(SiteNodeListParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeListParam");
		
		return new SiteNodeListQuery(
			param.cmpnyCd()
			, param.userCd()
			, param.siteCd()
			, param.nodeCd()
			, param.nodeType()
			, param.nodeNm()
			, param.parentNodeNm()
		); 
	}
}
