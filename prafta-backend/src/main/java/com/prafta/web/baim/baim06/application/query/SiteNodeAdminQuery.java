package com.prafta.web.baim.baim06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;

public record SiteNodeAdminQuery(
	String siteCd
	, String userCd
	, String gvCmpnyCd
){
	public static SiteNodeAdminQuery from(SiteNodeAdminParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteNodeAdminQuery(
    		param.siteCd()
    		, param.userCd()
    		, param.gvCmpnyCd()
        );        
    }
}
