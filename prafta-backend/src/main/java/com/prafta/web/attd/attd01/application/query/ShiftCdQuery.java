package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam;

public record ShiftCdQuery(
	String siteCd
	, String gvCmpnyCd
){
	public static ShiftCdQuery from(ShiftSchInfoParam param) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftSchInfoParam");

        return new ShiftCdQuery(
        	param.shiftType().siteCd()
        	, param.gvCmpnyCd()
        );
	}
}
