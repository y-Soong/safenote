package com.prafta.web.baim.baim03.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.application.param.TermsInfoParam;

public record TermsInfoCommand(
	String termsId
	, String termsNm
	, String termsVersion
	, String requiredYn
	, String termsContent
	, String strDate
	, String useYn
	, String termsDesc
	, String gvUserCd
){
	public static TermsInfoCommand from(TermsInfoParam param, String versionNo) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (versionNo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        return new TermsInfoCommand(
    		param.termsId()
        	, param.termsNm()
        	, versionNo
        	, param.requiredYn()
        	, param.termsContent()
        	, param.strDate()
        	, param.useYn()
        	, param.termsDesc()
        	, param.gvUserCd()
        );
    }
}