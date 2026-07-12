package com.prafta.web.tbm.tbm03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm03.application.param.UserProgressDetailParam;

public record UserProgressDetailQuery(
	String userCd
	, String userTypeCd
	, String startDate			// 이수일(STATUS_UPDATED_AT) YYYY-MM-DD
	, String endDate
	, String completionStatusCd
	, int offset
	, int pageSize
	, String gvCmpnyCd
){
	public static UserProgressDetailQuery from(UserProgressDetailParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		int offset = (param.page() - 1) * param.pageSize();

		return new UserProgressDetailQuery(
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
