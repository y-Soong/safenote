package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.SiteNodeAdminCandidateListRequest;

public record SiteNodeAdminCandidateListParam (
	String userId
	, String userNm
	, String siteCd
	, String nodeCd
	, String gvCmpnyCd
){
	public static SiteNodeAdminCandidateListParam from(SiteNodeAdminCandidateListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        System.out.println("param 팩토링 siteCd :: " + request.getSiteCd());
	
	    return new SiteNodeAdminCandidateListParam(
			request.getUserId()
			, request.getUserNm()
			, request.getSiteCd()
			, request.getNodeCd()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}
