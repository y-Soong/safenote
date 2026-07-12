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
	// PRAFTA-WEB_002-T1-02(1.3-3/1.4-1): 담당 미지정 노드 포함 여부(true=포함). 미전달은 false(현행).
	, boolean includeNoAdmin
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
			, Boolean.TRUE.equals(request.getIncludeNoAdmin())
		);
	}
}
