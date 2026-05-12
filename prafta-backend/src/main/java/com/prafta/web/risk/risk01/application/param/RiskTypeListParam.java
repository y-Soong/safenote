package com.prafta.web.risk.risk01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.dto.request.RiskTypeListRequest;

public record RiskTypeListParam(
	String processCd
	, String siteCd
	, String riskTypeNm
	, String useYn
	, String gvCmpnyCd
){
	public static RiskTypeListParam from(RiskTypeListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskTypeListRequest");

        return new RiskTypeListParam(
    		request.getProcessCd()
    		, request.getSiteCd()
    		, request.getRiskTypeNm()
    		, request.getUseYn()
    		, tokenInfo.gv_cmpnyCd()
        );
    }
}
