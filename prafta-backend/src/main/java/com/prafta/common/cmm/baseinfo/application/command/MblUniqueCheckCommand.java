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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new MblUniqueCheckCommand(
			smsId
			, mblNoHmac
			, param.certNo()
		); 
	}
}
