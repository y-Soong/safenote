package com.prafta.web.risk.risk01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.param.RiskTypeListParam;

public record RiskTypeListQuery(
		String processCd
		, String siteCd
		, String riskTypeNm
		, String useYn
		, String gvCmpnyCd
){
	public static RiskTypeListQuery from(RiskTypeListParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);


        return new RiskTypeListQuery(
    		param.processCd()
    		, param.siteCd()
    		, param.riskTypeNm()
    		, param.useYn()
    		, param.gvCmpnyCd()
        );
    }
}