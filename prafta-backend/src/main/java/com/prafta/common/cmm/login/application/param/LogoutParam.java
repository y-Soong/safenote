package com.prafta.common.cmm.login.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record LogoutParam(
	String cmpnyCd
	, String userCd
	, String clientType
	, String deviceId
) {
	public static LogoutParam from(String clientType, TokenInfo tokenInfo) {
		
		if(clientType == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - clientType");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
		
        return new LogoutParam(
    		tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , clientType
            , tokenInfo.gv_deviceId()
        );
    }
}
