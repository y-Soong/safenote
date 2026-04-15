package com.prafta.web.baim.baim04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim04.application.param.DailyUserLinkPoliciesParam;

public record DailyUserLinkPoliciesQuery(
		String siteCd
		, String useYn
		, String gvCmpnyCd
){
	public static DailyUserLinkPoliciesQuery from(DailyUserLinkPoliciesParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - DailyUserLinkPoliciesParam");

        return new DailyUserLinkPoliciesQuery(
        		param.siteCd()
        		, param.useYn()
        		, param.gvCmpnyCd()
		);
	}
}

