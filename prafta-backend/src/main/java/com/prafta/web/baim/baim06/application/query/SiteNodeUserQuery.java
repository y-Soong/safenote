package com.prafta.web.baim.baim06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;

public record SiteNodeUserQuery(
	String siteCd
	, String nodeCd
	, String userCd
	, String gvCmpnyCd
){
	public static SiteNodeUserQuery from(SiteNodeAdminParam param, String nodeCd) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeAdminParam");

        return new SiteNodeUserQuery(
    		param.siteCd()
    		, nodeCd
    		, param.userCd()
    		, param.gvCmpnyCd()
        );        
    }
}
