package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.SystInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SystInfoQuery(
	String codeD
	, String nameD
	, String code
){
	public static SystInfoQuery from(SystInfoParam param) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SystInfoQuery(
			param.codeD()
			, param.nameD()
			, param.code()
		);
	}
}
