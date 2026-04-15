package com.prafta.web.baim.baim06.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;

import lombok.Builder;
import lombok.Value;

public record UserNodeInfoQuery(
	String siteCd
	, String userCd
	, String gvCmpnyCd
){
	public static UserNodeInfoQuery from(SiteNodeAdminParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeAdminParam");

        return new UserNodeInfoQuery(
    		param.siteCd()
    		, param.userCd()
    		, param.gvCmpnyCd()
        );        
    }
}
