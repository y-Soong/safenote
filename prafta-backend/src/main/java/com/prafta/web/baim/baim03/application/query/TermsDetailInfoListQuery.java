package com.prafta.web.baim.baim03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.application.param.TermsDetailInfoListParam;

public record TermsDetailInfoListQuery(
	String termsId
	, String termsVersion
){
	public static TermsDetailInfoListQuery from(TermsDetailInfoListParam param) {

        // 1) 리스트 자체 검증
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TermsDetailInfoListQuery(
        		param.termsId()
        	, param.termsVersion()
        );
    }
}
