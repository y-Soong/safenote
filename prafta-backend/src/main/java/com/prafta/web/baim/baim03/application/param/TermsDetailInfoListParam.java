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
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TermsDetailInfoListParam(
        	request.getTermsId()
        	, request.getTermsVersion()
        );
    }
}
