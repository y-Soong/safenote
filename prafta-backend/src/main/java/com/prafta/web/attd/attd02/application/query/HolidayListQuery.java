package com.prafta.web.attd.attd02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd02.application.param.HolidayListParam;

public record HolidayListQuery(
	String year
	, String month
	, String gvCmpnyCd
){
	public static HolidayListQuery from(HolidayListParam param) {
		
		if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - HolidayListParam");
		
		return new HolidayListQuery(
			param.year()
			, param.month()
			, param.gvCmpnyCd()
		);
	}
}
