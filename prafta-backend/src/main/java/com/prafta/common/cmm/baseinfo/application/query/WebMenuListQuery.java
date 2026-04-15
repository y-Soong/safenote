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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - WebMenuListParam");
		
		return new WebMenuListQuery(
			param.cmpnyCd()
			, param.userCd()
		);
	}
}
