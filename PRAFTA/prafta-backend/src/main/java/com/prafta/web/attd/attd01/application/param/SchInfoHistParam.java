package com.prafta.web.attd.attd01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.SchInfoHistRequest;

public record SchInfoHistParam(
	String siteCd
	, String schCd
	, String gvCmpnyCd
){
	public static SchInfoHistParam from(SchInfoHistRequest request, TokenInfo tokenInfo) {
		
        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoHistParam(
        		request.getSiteCd()
        		, request.getSchCd()
        		, tokenInfo.gv_cmpnyCd()
        );
	}
}
