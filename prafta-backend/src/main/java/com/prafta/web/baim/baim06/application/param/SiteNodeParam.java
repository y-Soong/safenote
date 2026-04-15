package com.prafta.web.baim.baim06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.dto.request.SiteNodeRequest;

public record SiteNodeParam(
	String siteCd
	, String nodeCd
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static SiteNodeParam from(SiteNodeRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        return new SiteNodeParam(
    		request.getSiteCd()
    		, request.getNodeCd()
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
        );        
    }
}
