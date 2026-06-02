package com.prafta.web.baim.baim05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.DailyUserLinkPoliciesRequest;

public record DailyUserLinkPoliciesParam(
		String siteCd
		, String gvCmpnyCd
){
	public static DailyUserLinkPoliciesParam from(DailyUserLinkPoliciesRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new DailyUserLinkPoliciesParam(
        		request.getSiteCd()
        		, tokenInfo.gv_cmpnyCd()
		);
	}
}
