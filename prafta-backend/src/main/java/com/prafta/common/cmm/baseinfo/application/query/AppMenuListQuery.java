package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.AppMenuListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record AppMenuListQuery(
	String cmpnyCd
	, String userCd
) {
	public static AppMenuListQuery from(AppMenuListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new AppMenuListQuery(
			param.cmpnyCd()
			, param.userCd()
		);
	}
}
