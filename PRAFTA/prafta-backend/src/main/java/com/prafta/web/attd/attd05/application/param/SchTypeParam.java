package com.prafta.web.attd.attd05.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;
import com.prafta.web.attd.attd05.dto.request.SchTypeRequst;

public record SchTypeParam(
	List<SchTypeModel> schTypeModelList
	, String gvAuthCd
) {

	public static SchTypeParam from(List<SchTypeRequst> requests, TokenInfo tokenInfo) {

        if (requests == null || tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        List<SchTypeModel> models = requests.stream()
    		.map(req -> {
    			return new SchTypeModel(
    				req.getSiteCd()
    				, req.getUserCd()
    				, req.getWorkYmd()
    				, req.getWorkPlanCd()
    				, tokenInfo.gv_cmpnyCd()
    				, tokenInfo.gv_userCd()
				);
    		})
        	.toList();

        return new SchTypeParam(models, tokenInfo.gv_authCd());
    }
}
