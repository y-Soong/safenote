package com.prafta.web.risk.risk03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.application.param.RiskAssessmentsListParam;

public record RiskAssessmentsListQuery(
	String siteCd
	, String assessmentStatus
	, String processCd
	, String riskTypeCd
	, String gvCmpnyCd
){
	public static RiskAssessmentsListQuery from(RiskAssessmentsListParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new RiskAssessmentsListQuery(
    		param.siteCd()
        	, param.assessmentStatus()
        	, param.processCd()
        	, param.riskTypeCd()
        	, param.gvCmpnyCd()
        );
    }
}
