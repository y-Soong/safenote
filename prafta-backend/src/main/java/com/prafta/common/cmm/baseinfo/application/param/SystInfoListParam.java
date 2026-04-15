package com.prafta.common.cmm.baseinfo.application.param;

import java.util.List;

import com.prafta.common.cmm.baseinfo.dto.request.SystInfoListRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SystInfoListParam (
		List<String> systCodeList
) {
	public static SystInfoListParam from(SystInfoListRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SystInfoListRequest");
		
		return new SystInfoListParam(
				request.getSystCodeList()
		);
	}
}
