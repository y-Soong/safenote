package com.prafta.web.risk.risk03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.application.param.RiskTypeInfoListParam;

public record RiskTypeInfoListQuery(
	String gvCmpnyCd
){
	public static RiskTypeInfoListQuery from(RiskTypeInfoListParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new RiskTypeInfoListQuery(param.gvCmpnyCd());
    }
}
