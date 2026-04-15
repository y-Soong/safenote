package com.prafta.web.baim.baim06.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.model.SiteNodeModel;
import com.prafta.web.baim.baim06.dto.request.SiteNodeInfoRequest;

public record SiteNodeInfoParam(
	List<SiteNodeModel> siteNodeModelList
) {
	public static SiteNodeInfoParam from(List<SiteNodeInfoRequest> requests, TokenInfo tokenInfo) {

        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SiteNodeRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
        
        List<SiteNodeModel> models = requests.stream()
    		.map(req -> {
    			return new SiteNodeModel(
    				req.getSiteCd()
    				, req.getNodeCd()
    				, req.getNodeNm()
    				, req.getNodeType()
    				, req.getParentNodeCd()
    				, req.getSelfAttdApprvYn() 
    				, tokenInfo.gv_cmpnyCd()
    				, tokenInfo.gv_userCd()
				);
    		})
        	.toList();

        return new SiteNodeInfoParam(models);        
    }
}
