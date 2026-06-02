package com.prafta.web.risk.risk01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.param.RiskHazardListParam;

public record RiskHazardListQuery(
	String riskTypeCd
	, String hazardNm
	, String hazardDesc
	, String gvCmpnyCd
){
	public static RiskHazardListQuery from(RiskHazardListParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new RiskHazardListQuery(
    		param.riskTypeCd()
    		, param.hazardNm()
    		, param.hazardDesc()
    		, param.gvCmpnyCd()
        );
    }
}
