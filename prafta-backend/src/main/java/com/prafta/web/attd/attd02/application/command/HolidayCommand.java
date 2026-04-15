package com.prafta.web.attd.attd02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd02.application.param.HolidayParam;

public record HolidayCommand(
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
	public static HolidayCommand from(HolidayParam param, String holidayId, String holidayType) {
		
		if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - HolidayParam");
		if (holidayId == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - holidayId");
		if (holidayType == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - holidayType");
		
		return new HolidayCommand(
			param.siteCd()
			, holidayId
			, param.holidayNm()
			, param.holidayYmd()
			, holidayType
			, param.repeatYearly()
			, param.useYn()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}