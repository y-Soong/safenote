package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.WebMenuListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record WebMenuListQuery(
	String cmpnyCd
	, String userCd
) {
	public static WebMenuListQuery from(WebMenuListParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new WebMenuListQuery(
			param.cmpnyCd()
			, param.userCd()
		);
	}
}
