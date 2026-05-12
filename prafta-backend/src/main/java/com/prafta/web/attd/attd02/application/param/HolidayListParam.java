package com.prafta.web.attd.attd02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd02.dto.request.HolidayListRequest;

public record HolidayListParam(
	String year
	, String month
	, String gvCmpnyCd
){
	public static HolidayListParam from(HolidayListRequest request, TokenInfo tokenInfo) {
		
		if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - HolidayListRequest");
		
		return new HolidayListParam(
			request.getYear()
			, request.getMonth()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}
