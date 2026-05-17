package com.prafta.web.baim.baim05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;

public record DailyUserLinkPoliciesQuery(
		String siteCd
		, String gvCmpnyCd
){
	public static DailyUserLinkPoliciesQuery from(DailyUserLinkPoliciesParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DailyUserLinkPoliciesQuery(
        		param.siteCd()
        		, param.gvCmpnyCd()
		);
	}

}

