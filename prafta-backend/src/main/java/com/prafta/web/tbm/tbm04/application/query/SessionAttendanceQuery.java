package com.prafta.web.tbm.tbm04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm04.application.param.SessionAttendanceParam;

public record SessionAttendanceQuery(
	String sessionCd
	, String userTypeCd
	, String completionStatusCd
	, String gvCmpnyCd
){
	public static SessionAttendanceQuery from(SessionAttendanceParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new SessionAttendanceQuery(
			param.sessionCd()
			, param.userTypeCd()
			, param.completionStatusCd()
			, param.gvCmpnyCd()
		);
	}
}
