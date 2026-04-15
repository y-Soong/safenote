package com.prafta.web.baim.baim04.application.param;


import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim04.application.model.LinkPoliciesModel;

public record UserSlotCountQuery(
	String siteCd
	, String gvCmpnyCd
){
	public static UserSlotCountQuery from(LinkPoliciesModel model) {

        if (model == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - LinkPoliciesModel");

        return new UserSlotCountQuery(
    		model.siteCd()
    		, model.gvCmpnyCd()
        );        
    }
}
