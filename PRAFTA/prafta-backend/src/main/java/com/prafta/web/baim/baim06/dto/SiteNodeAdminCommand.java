package com.prafta.web.baim.baim06.dto;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;

public record SiteNodeAdminCommand(
	String siteCd
	, String nodeCd
	, String userCd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SiteNodeAdminCommand from(SiteNodeAdminParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteNodeAdminCommand(
    		param.siteCd()
    		, param.nodeCd()
    		, param.userCd()
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );        
    }
}
