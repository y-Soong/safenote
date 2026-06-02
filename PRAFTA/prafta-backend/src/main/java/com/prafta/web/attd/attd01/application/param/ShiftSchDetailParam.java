package com.prafta.web.attd.attd01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.ShiftSchDetailRequest;

public record ShiftSchDetailParam(
	String siteCd
	, String shiftCd
	, String gvCmpnyCd
){
	public static ShiftSchDetailParam from(ShiftSchDetailRequest request, TokenInfo tokenInfo) {
		
        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftSchDetailParam(
    		request.getSiteCd()
    		, request.getShiftCd()
    		, tokenInfo.gv_cmpnyCd()
        );
	}
}
