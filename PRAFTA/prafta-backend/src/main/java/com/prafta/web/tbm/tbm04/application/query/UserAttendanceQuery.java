package com.prafta.web.tbm.tbm04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.application.param.UserAttendanceParam;

public record UserAttendanceQuery(
	String userCd
	, String userTypeCd
	, String startDate			// YYYY-MM-DD
	, String endDate			// YYYY-MM-DD
	, String completionStatusCd
	, int offset
	, int pageSize
	, String gvCmpnyCd
){
	public static UserAttendanceQuery from(UserAttendanceParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		int offset = (param.page() - 1) * param.pageSize();

		return new UserAttendanceQuery(
			param.userCd()
			, param.userTypeCd()
			, param.startDate()
			, param.endDate()
			, param.completionStatusCd()
			, offset
			, param.pageSize()
			, param.gvCmpnyCd()
		);
	}
}
