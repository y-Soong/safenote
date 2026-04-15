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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNoEnc");
		if(mblNoHmac == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNoHmac");
		if(certNo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - certNo");
		
		return new SmsAuthNoCommand(
			mblNoEnc
			, mblNoHmac
			, certNo
		); 
	}
}
