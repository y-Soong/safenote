package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.TermsDetailInfoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record TermsDetailInfoParam(
	String termsId
) {
	public static TermsDetailInfoParam from(TermsDetailInfoRequest request) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsDetailInfoRequest");
		
		return new TermsDetailInfoParam(
			request.getTermsId()
		); 
	}
}
