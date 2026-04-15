package com.prafta.web.risk.risk03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.application.param.RiskTypeInfoListParam;

public record RiskTypeInfoListQuery(
	String gvCmpnyCd
){
	public static RiskTypeInfoListQuery from(RiskTypeInfoListParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - RiskTypeInfoListParam");

        return new RiskTypeInfoListQuery(param.gvCmpnyCd());
    }
}
