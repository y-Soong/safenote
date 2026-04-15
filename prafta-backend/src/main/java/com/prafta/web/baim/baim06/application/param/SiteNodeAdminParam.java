package com.prafta.web.baim.baim06.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.dto.request.SiteNodeAdminRequest;

public record SiteNodeAdminParam(
	String siteCd
	, String nodeCd
	, String userCd
	, String userNm
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SiteNodeAdminParam from(SiteNodeAdminRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeAdminRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
        
        return new SiteNodeAdminParam(
    		request.getSiteCd()
    		, request.getNodeCd()
    		, request.getUserCd()
    		, request.getUserNm()
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
        );        
    }
}
