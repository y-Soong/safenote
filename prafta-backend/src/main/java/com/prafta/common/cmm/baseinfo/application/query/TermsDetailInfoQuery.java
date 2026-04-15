package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.TermsDetailInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record TermsDetailInfoQuery(
	String termsId
) {
	public static TermsDetailInfoQuery from(TermsDetailInfoParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsDetailInfoParam");
		
		return new TermsDetailInfoQuery(
			param.termsId()
		); 
	}
}
