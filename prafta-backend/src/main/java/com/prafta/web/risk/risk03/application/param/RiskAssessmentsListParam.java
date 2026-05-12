package com.prafta.web.risk.risk03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.dto.request.RiskAssessmentsListRequest;

public record RiskAssessmentsListParam(
	String siteCd
	, String assessmentStatus
	, String processCd
	, String riskTypeCd
	, String gvCmpnyCd
){
	public static RiskAssessmentsListParam from(RiskAssessmentsListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskAssessmentsListRequest");


        return new RiskAssessmentsListParam(
        	request.getSiteCd()
        	, request.getAssessmentStatus()
        	, request.getProcessCd()
        	, request.getRiskTypeCd()
        	, tokenInfo.gv_cmpnyCd()
        );
    }
}
