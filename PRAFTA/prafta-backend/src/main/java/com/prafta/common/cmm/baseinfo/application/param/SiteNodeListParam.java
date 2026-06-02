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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(token == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new SiteNodeListParam(
			token.gv_cmpnyCd()
			, token.gv_userCd()
			, request.getSiteCd()
			, request.getNodeCd()
			, request.getNodeType()
			, request.getNodeNm()
			, request.getParentNodeNm()
		); 
	}
}
