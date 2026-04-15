package com.prafta.web.risk.risk01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.model.RiskTypeModel;

public record RiskHazardCountQuery(
	String riskTypeCd
	, String gvCmpnyCd
){
	public static RiskHazardCountQuery from(RiskTypeModel model) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskTypeModel");

        return new RiskHazardCountQuery(
        	model.riskTypeCd()
        	, model.gvCmpnyCd()
        );
    }
}