package com.prafta.web.risk.risk01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.dto.request.RiskHazardListRequest;

public record RiskHazardListParam(
	String riskTypeCd
	, String hazardNm
	, String hazardDesc
	, String gvCmpnyCd
){
	public static RiskHazardListParam from(RiskHazardListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskHazardListRequest");

        return new RiskHazardListParam(
        		request.getRiskTypeCd()
        		, request.getHazardNm()
        		, request.getHazardDesc()
        		, tokenInfo.gv_cmpnyCd()
        );
    }
}