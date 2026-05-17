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
            throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (holidayId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (holidayType == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
		
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