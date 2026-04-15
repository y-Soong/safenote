package com.prafta.web.baim.baim03.application.param;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.dto.request.TermsInfoListRequest;

public record TermsInfoListParam(
	String termsId
	, String termsNm
){
	public static TermsInfoListParam from(TermsInfoListRequest request) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsInfoListRequest");

        return new TermsInfoListParam(
        	request.getTermsId()
        	, request.getTermsNm()
        );
    }
}
