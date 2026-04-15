package com.prafta.common.cmm.baseinfo.application.command;

import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record MblUniqueCheckCommand(
	String smsId
    , String mblNoHmac
    , String certNo
) {
	public static MblUniqueCheckCommand from(String smsId, String mblNoHmac, UserSmsAuthNoCheckParam param) {
		
		if(smsId == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - smsId");
		if(mblNoHmac == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNoHmac");
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserSmsAuthNoCheckParam");
		
		return new MblUniqueCheckCommand(
			smsId
			, mblNoHmac
			, param.certNo()
		); 
	}
}
