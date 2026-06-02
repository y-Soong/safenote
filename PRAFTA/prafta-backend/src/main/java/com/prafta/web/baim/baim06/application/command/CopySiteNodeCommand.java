package com.prafta.web.baim.baim06.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.param.CopySiteNodeParam;


public record CopySiteNodeCommand(
	String siteCd
	, String targetSiteCd
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static CopySiteNodeCommand from(CopySiteNodeParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new CopySiteNodeCommand(
    		param.siteCd()
        	, param.targetSiteCd()
        	, param.gvCmpnyCd()
        	, param.gvUserCd()
        );        
    }
}
