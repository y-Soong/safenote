package com.prafta.web.baim.baim03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.dto.request.TermsInfoRequest;

public record TermsInfoParam(
	String termsId
	, String termsNm
	, String requiredYn
	, String termsContent
	, String strDate
	, String useYn
	, String termsDesc
	, String gvCmpnyCd
	, String gvUserCd
){
	public static TermsInfoParam from(TermsInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsInfoRequest");

        return new TermsInfoParam(
        		request.getTermsId()
        		, request.getTermsNm()
        		, request.getRequiredYn()
        		, request.getTermsContent()
        		, request.getStrDate()
        		, request.getUseYn()
        		, request.getTermsDesc()
        		, tokenInfo.gv_cmpnyCd()
        		, tokenInfo.gv_userCd()
        );        
    }
}