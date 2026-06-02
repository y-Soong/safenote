package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.MenuListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record MenuListParam(
	String cmpnyCd
	, String userCd
	, String menuSrc
	
) {
	public static MenuListParam from(MenuListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		
        return new MenuListParam(
        	tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
    		, request.getMenuSrc()
        );
    }
}
