package com.prafta.web.baim.baim04.application.param;


import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim04.application.model.LinkPoliciesModel;
import com.prafta.web.baim.baim04.dto.request.LinkPoliciesRequest;

public record LinkPoliciesParam(
	List<LinkPoliciesModel> linkPoliciesModelList 
){
	public static LinkPoliciesParam from(List<LinkPoliciesRequest> requests, TokenInfo tokenInfo) {

        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TermsInfoRequest");
        
        List<LinkPoliciesModel> models = requests.stream()
    		.map(req -> {
    			return new LinkPoliciesModel(
    				req.getChk()
    				, req.getCmpnyCd()
    				, req.getSiteCd()
    				, req.getUseYn()
    				, req.getDayLimitCnt()
    				, req.getServiceUrl() 
    				, tokenInfo.gv_cmpnyCd()
    				, tokenInfo.gv_userCd()
				);
    		})
        	.toList();

        return new LinkPoliciesParam(models);        
    }
}
