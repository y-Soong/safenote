package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoListParam;

public record ShiftSchInfoListQuery(
	String siteCd
	, String shiftNo
	, String shiftCycleDays
	, String useYn
	, String gvCmpnyCd
){
	public static ShiftSchInfoListQuery from(ShiftSchInfoListParam param) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftSchInfoListQuery(
    		param.siteCd()
    		, param.shiftNo()
    		, param.shiftCycleDays()
    		, param.useYn()
    		, param.gvCmpnyCd()
        );
	}
}
