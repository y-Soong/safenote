package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;


public record SchNoCountQuery(
	String schNo
	, String siteCd
	, String gvCmpnyCd
){
	public static SchNoCountQuery from(SchInfoParam param) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchNoCountQuery(
    		param.schNo()
    		, param.siteCd()
    		, param.gvCmpnyCd()
        );
	}
}
