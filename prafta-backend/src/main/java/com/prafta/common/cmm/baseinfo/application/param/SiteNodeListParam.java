package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.SiteNodeListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteNodeListParam(
	String cmpnyCd
	, String userCd
	, String siteCd
	, String nodeCd
	, String nodeType
	, String nodeNm
	, String parentNodeNm	
) {
	public static SiteNodeListParam from(SiteNodeListRequest request, TokenInfo token) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeListRequest");
		
		return new SiteNodeListParam(
			token != null ? token.gv_cmpnyCd() : request.getCmpnyCd()
			, token != null ? token.gv_userCd() : ""
			, request.getSiteCd()
			, request.getNodeCd()
			, request.getNodeType()
			, request.getNodeNm()
			, request.getParentNodeNm()
		); 
	}
}
