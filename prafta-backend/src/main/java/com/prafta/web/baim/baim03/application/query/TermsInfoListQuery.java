package com.prafta.web.baim.baim03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.application.param.TermsInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoParam;

public record TermsInfoListQuery(
	String termsId
	, String termsNm
){
	public static TermsInfoListQuery from(TermsInfoListParam p) {
        if (p == null) throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - TermsInfoListParam");
        return new TermsInfoListQuery(
    		p.termsId()
    		, p.termsNm()
		);
    }

    public static TermsInfoListQuery from(TermsInfoParam p) {
        if (p == null) throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - TermsInfoParam");
        return new TermsInfoListQuery(
    		p.termsId()
    		, p.termsNm()
		); // TermsInfoParam에 매핑 가능한 필드만
    }
}
