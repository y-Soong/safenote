package com.prafta.web.attd.attd01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.ShiftSchInfoListRequest;

public record ShiftSchInfoListParam(
	String siteCd
	, String shiftNo
	, String shiftCycleDays
	, String useYn
	, String gvCmpnyCd
){
	public static ShiftSchInfoListParam from(ShiftSchInfoListRequest request, TokenInfo tokenInfo) {
		
        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftSchInfoListRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        return new ShiftSchInfoListParam(
    		request.getSiteCd()
    		, request.getShiftNo()
    		, request.getShiftCycleDays()
    		, request.getUseYn()
    		, tokenInfo.gv_cmpnyCd()
        );
	}
}
