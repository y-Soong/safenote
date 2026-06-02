package com.prafta.web.baim.baim05.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;

public record LinkPoliciesCommand(
		String siteCd
		, String useYn
		, String dayLimitCnt
		, String gvCmpnyCd
		, String gvUserCd
	){
		public static LinkPoliciesCommand from(LinkPoliciesParam param) {

	        if (param == null)
	        	throw new ApiException(CommonErrorCode.COMMON_400_001);

	        return new LinkPoliciesCommand(
	    		param.siteCd()
				, param.useYn()
				, param.dayLimitCnt()
				, param.gvCmpnyCd()
				, param.gvUserCd()
	        );
	    }
	}
