package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;

public record SiteNodeAdminCandidateListQuery(
	String userId
	, String userNm
	, String siteCd
	, String nodeCd
	, String gvCmpnyCd
){
	public static SiteNodeAdminCandidateListQuery from(SiteNodeAdminCandidateListParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeAdminCandidateListParam");
	
	    return new SiteNodeAdminCandidateListQuery(
			param.userId()
			, param.userNm()
			, param.siteCd()
			, param.nodeCd()
			, param.gvCmpnyCd()
		);
	}	
}

