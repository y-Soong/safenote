package com.prafta.web.baim.baim04.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim04.application.model.LinkPoliciesModel;

public record LinkPoliciesCommand(
	String chk
	, String cmpnyCd
	, String siteCd
	, String useYn
	, String dayLimitCnt
	, String serviceUrl
	, String gvCmpnyCd
	, String gvUserCd
){
	public static LinkPoliciesCommand from(LinkPoliciesModel model) {

        if (model == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        

        return new LinkPoliciesCommand(
    		model.chk()
			, model.cmpnyCd()
			, model.siteCd()
			, model.useYn()
			, model.dayLimitCnt()
			, model.serviceUrl() 
			, model.gvCmpnyCd()
			, model.gvUserCd()
        );
    }
}
