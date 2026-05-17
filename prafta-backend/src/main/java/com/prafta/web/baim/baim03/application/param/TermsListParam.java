package com.prafta.web.baim.baim03.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.application.model.TermsModel;
import com.prafta.web.baim.baim03.dto.request.TermsInfoRequest;

public record TermsListParam(
	List<TermsModel> termsInfoModelList
){
	public static TermsListParam from(List<TermsInfoRequest> requests, TokenInfo tokenInfo) {

        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        List<TermsModel> models = requests.stream()
    		.map(req -> {
    			return new TermsModel(
	    			req.getTermsId()
	        		, req.getTermsNm()
	        		, req.getRequiredYn()
	        		, req.getTermsContent()
	        		, req.getStrDate()
	        		, req.getUseYn()
	        		, req.getTermsDesc()
	        		, tokenInfo.gv_cmpnyCd()
	        		, tokenInfo.gv_userCd()
				);
    		})
        	.toList();

        return new TermsListParam(models);
    }
}