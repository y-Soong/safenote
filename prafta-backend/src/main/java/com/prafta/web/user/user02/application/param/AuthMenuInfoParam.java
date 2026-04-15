package com.prafta.web.user.user02.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user02.application.model.AuthMenuInfoModel;
import com.prafta.web.user.user02.dto.request.AuthMenuInfoRequest;

public record AuthMenuInfoParam(
	List<AuthMenuInfoModel> authMenuInfoModelList
){	
	public static AuthMenuInfoParam from(List<AuthMenuInfoRequest> requests, TokenInfo tokenInfo) {
		
		if(requests == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - AuthMenuInfoRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo	");
		
		List<AuthMenuInfoModel> models = requests.stream()
            .map(request -> {
                return new AuthMenuInfoModel(
            		request.getAuthCd()
        			, request.getMenuDId()
        			, request.getUseYn()
        			, request.getBtnSrch()
        			, request.getBtnNew()
        			, request.getBtnDel()
        			, request.getBtnSave()
        			, request.getBtnExcl()
        			, tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();
		
		return new AuthMenuInfoParam(models); 
	}
}
