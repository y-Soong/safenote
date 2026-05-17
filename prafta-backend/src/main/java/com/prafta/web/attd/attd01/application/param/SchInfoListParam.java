package com.prafta.web.attd.attd01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.SchInfoListRequest;

public record SchInfoListParam(
		String siteCd
		, String schNo
		, String schType
		, String useYn
		, String gvCmpnyCd
){
	public static SchInfoListParam from(SchInfoListRequest request, TokenInfo tokenInfo) {
		
        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);


        return new SchInfoListParam(
    		request.getSiteCd()
    		, request.getSchNo()
    		, request.getSchType()
    		, request.getUseYn()
    		, tokenInfo.gv_cmpnyCd()
        );
	}
}
