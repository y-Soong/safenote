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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
        return new LogoutParam(
    		tokenInfo != null ? tokenInfo.gv_cmpnyCd() : ""
            , tokenInfo != null ? tokenInfo.gv_userCd() : "SYSTEM"
            , clientType
            , tokenInfo != null ? tokenInfo.gv_deviceId() : ""
        );
    }
}
