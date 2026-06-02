package com.prafta.web.baim.baim06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.CopySiteNodeParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeParam;

public record SiteNodeCommand(
	String siteCd
	, String nodeCd
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static SiteNodeCommand from(SiteNodeParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteNodeCommand(
    		param.siteCd()
    		, param.nodeCd()
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );        
    }
	
	public static SiteNodeCommand from(CopySiteNodeParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteNodeCommand(
    		param.siteCd()
    		, ""
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );        
    }
}
