package com.prafta.web.tbm.tbm02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.application.param.SessionDetailParam;

public record SessionDetailQuery(
	String sessionCd
	, String gvCmpnyCd
){
	public static SessionDetailQuery from(SessionDetailParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new SessionDetailQuery(
			param.sessionCd()
			, param.gvCmpnyCd()
		);
	}
}
