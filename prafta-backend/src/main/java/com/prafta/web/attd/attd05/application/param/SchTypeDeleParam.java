package com.prafta.web.attd.attd05.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.model.SchTypeDeleModel;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;
import com.prafta.web.attd.attd05.dto.request.SchTypeDeleRequst;

public record SchTypeDeleParam(
	List<SchTypeDeleModel> schTypeDeleModelList
) {
	
	public static SchTypeDeleParam from(List<SchTypeDeleRequst> requests, TokenInfo tokenInfo) {

        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchTypeDeleRequst");
        
        List<SchTypeDeleModel> models = requests.stream()
    		.map(req -> {
    			return new SchTypeDeleModel(
    				req.getSiteCd()
    				, req.getUserCd()
    				, req.getWorkYm()
    				, tokenInfo.gv_cmpnyCd()
    				, tokenInfo.gv_userCd()
				);
    		})
        	.toList();

        return new SchTypeDeleParam(models);        
    }
}
