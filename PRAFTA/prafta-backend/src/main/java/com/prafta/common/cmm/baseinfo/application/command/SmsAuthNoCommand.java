package com.prafta.common.cmm.baseinfo.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SmsAuthNoCommand(
	String mblNoEnc
    , String mblNoHmac
    , String certNo	
) {
	public static SmsAuthNoCommand from(String mblNoEnc, String mblNoHmac, String certNo) {
		
		if(mblNoEnc == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(certNo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new SmsAuthNoCommand(
			mblNoEnc
			, mblNoHmac
			, certNo
		); 
	}
}
