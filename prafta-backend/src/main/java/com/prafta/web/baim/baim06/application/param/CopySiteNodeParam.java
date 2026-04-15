package com.prafta.web.baim.baim06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.dto.request.CopySiteNodeRequest;


public record CopySiteNodeParam(
	String siteCd
	, String targetSiteCd
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static CopySiteNodeParam from(CopySiteNodeRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - CopySiteNodeRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        return new CopySiteNodeParam(
        	request.getSiteCd()
        	, request.getTargetSiteCd()
        	, tokenInfo.gv_cmpnyCd()
        	, tokenInfo.gv_userCd()
        );        
    }
}
