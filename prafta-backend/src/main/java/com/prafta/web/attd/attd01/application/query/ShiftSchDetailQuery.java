package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchDetailParam;

public record ShiftSchDetailQuery(
	String siteCd
	, String shiftCd
	, String gvCmpnyCd
){
	public static ShiftSchDetailQuery from(ShiftSchDetailParam param) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftSchDetailParam");

        return new ShiftSchDetailQuery(
    		param.siteCd()
    		, param.shiftCd()
    		, param.gvCmpnyCd()
        );
	}
}
