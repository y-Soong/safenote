package com.prafta.web.baim.baim03.application.param;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.dto.request.TermsDetailInfoListRequest;

public record TermsDetailInfoListParam(
	String termsId
	, String termsVersion
){
	public static TermsDetailInfoListParam from(TermsDetailInfoListRequest request) {

        // 1) 리스트 자체 검증
        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsDetailInfoListRequest");

        return new TermsDetailInfoListParam(
        	request.getTermsId()
        	, request.getTermsVersion()
        );
    }
}
