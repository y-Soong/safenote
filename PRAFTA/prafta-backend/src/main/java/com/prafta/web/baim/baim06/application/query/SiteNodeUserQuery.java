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
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteNodeUserQuery(
    		param.siteCd()
    		, nodeCd
    		, param.userCd()
    		, param.gvCmpnyCd()
        );        
    }
}
