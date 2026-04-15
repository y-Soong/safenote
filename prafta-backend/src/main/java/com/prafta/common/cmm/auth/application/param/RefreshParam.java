package com.prafta.common.cmm.auth.application.param;

import com.prafta.common.cmm.auth.dto.request.RefreshRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record RefreshParam (
	String refreshToken
){
	public static RefreshParam from(RefreshRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RefreshRequest");
		
		return new RefreshParam(request.getRefreshToken());
	}
}
