package com.prafta.web.attd.attd02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd02.dto.request.HolidayRequest;

public record HolidayParam(
	String siteCd
	, String holidayId
	, String holidayNm
	, String holidayYmd
	, String holidayType
	, boolean repeatYearly
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static HolidayParam from(HolidayRequest request, TokenInfo tokenInfo) {
		
		if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - HolidayRequest");
		
		return new HolidayParam(
			request.getSiteCd()
			, request.getHolidayId()
			, request.getHolidayNm()
			, request.getHolidayYmd()
			, request.getHolidayType()
			, request.isRepeatYearly()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}