package com.prafta.web.baim.baim05.application.query;


import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;

public record UserSlotCountQuery(
	String siteCd
	, String gvCmpnyCd
){
	public static UserSlotCountQuery from(LinkPoliciesParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserSlotCountQuery(
    		param.siteCd()
    		, param.gvCmpnyCd()
        );        
    }
}
