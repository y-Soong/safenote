package com.prafta.web.tbm.tbm04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.application.param.AttendanceEventParam;

public record AttendanceEventQuery(
	String attendanceCd
	, int offset
	, int pageSize
	, String gvCmpnyCd
){
	public static AttendanceEventQuery from(AttendanceEventParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		int offset = (param.page() - 1) * param.pageSize();

		return new AttendanceEventQuery(
			param.attendanceCd()
			, offset
			, param.pageSize()
			, param.gvCmpnyCd()
		);
	}
}
