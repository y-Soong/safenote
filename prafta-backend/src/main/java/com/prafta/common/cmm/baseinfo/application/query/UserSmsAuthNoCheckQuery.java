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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserSmsAuthNoCheckParam");
		if(mblNoHmac == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNoHmac");
		
		return new UserSmsAuthNoCheckQuery(
				param.cmpnyCd()
				, mblNoHmac
				, param.certNo()
		); 
	}
}
