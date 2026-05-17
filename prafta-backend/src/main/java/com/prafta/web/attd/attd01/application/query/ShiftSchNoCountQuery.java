package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftTypeParam;


public record ShiftSchNoCountQuery(
	String shiftNo
	, String siteCd
	, String gvCmpnyCd
){
	public static ShiftSchNoCountQuery from(ShiftTypeParam param, String cmpnyCd) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftSchNoCountQuery(
    		param.shiftNo()
    		, param.siteCd()
    		, cmpnyCd
        );
	}
}
