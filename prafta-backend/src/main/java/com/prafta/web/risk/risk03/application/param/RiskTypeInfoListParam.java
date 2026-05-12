package com.prafta.web.risk.risk03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record RiskTypeInfoListParam(
	String gvCmpnyCd
){
	public static RiskTypeInfoListParam from(TokenInfo tokenInfo) {


        return new RiskTypeInfoListParam(tokenInfo.gv_cmpnyCd());
    }
}
