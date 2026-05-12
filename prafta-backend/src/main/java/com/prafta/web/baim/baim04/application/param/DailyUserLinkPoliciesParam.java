package com.prafta.web.baim.baim04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim04.dto.request.DailyUserLinkPoliciesRequest;

public record DailyUserLinkPoliciesParam(
		String siteCd
		, String useYn
		, String gvCmpnyCd
){
	public static DailyUserLinkPoliciesParam from(DailyUserLinkPoliciesRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - DailyUserLinkPoliciesRequest");

        return new DailyUserLinkPoliciesParam(
        		request.getSiteCd()
        		, request.getUseYn()
        		, tokenInfo.gv_cmpnyCd()
		);
	}
}
