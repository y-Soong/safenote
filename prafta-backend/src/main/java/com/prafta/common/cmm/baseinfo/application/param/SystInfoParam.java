package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.SystInfoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SystInfoParam(
		String codeD
		, String nameD
		, String code
){
	public static SystInfoParam from(SystInfoRequest request) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SystInfoParam(
			request.getCodeD()
			, request.getNameD()
			, request.getCode()
		);		
	}
}
