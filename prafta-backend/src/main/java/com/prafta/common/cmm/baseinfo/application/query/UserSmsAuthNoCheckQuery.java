package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserSmsAuthNoCheckQuery(
	String cmpnyCd
    , String mblNoHmac
    , String certNo
) {
	public static UserSmsAuthNoCheckQuery from(UserSmsAuthNoCheckParam param, String mblNoHmac) {
		
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new UserSmsAuthNoCheckQuery(
				param.cmpnyCd()
				, mblNoHmac
				, param.certNo()
		); 
	}
}
