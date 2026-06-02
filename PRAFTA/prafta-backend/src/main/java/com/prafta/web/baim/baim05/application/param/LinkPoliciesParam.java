package com.prafta.web.baim.baim05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.dto.request.LinkPoliciesRequest;

public record LinkPoliciesParam(
	String siteCd
	, String useYn
	, String dayLimitCnt
	, String gvCmpnyCd
	, String gvUserCd
){
	public static LinkPoliciesParam from(LinkPoliciesRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        return new LinkPoliciesParam(
    		request.getSiteCd()
    		, request.getUseYn()
    		, request.getDayLimitCnt()
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
		);
    }
}
