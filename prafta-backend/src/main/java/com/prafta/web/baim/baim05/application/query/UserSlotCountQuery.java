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
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - LinkPoliciesParam");

        return new UserSlotCountQuery(
    		param.siteCd()
    		, param.gvCmpnyCd()
        );        
    }
}
