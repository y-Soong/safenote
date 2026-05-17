package com.prafta.web.attd.attd05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.dto.request.SchTypeListRequst;


public record SchTypeListParam (
	String siteCd
	, String gvCmpnyCd
){
	public static SchTypeListParam from(SchTypeListRequst request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchTypeListParam(
    		request.getSiteCd()
    		, tokenInfo.gv_cmpnyCd()
		);
    }
}
