package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record MblUniqueCheckQuery(
		String mblNoHmac     // 사용자가 입력한 번호 (원본)
) {
	public static MblUniqueCheckQuery from(String mblNoHmac) {
		
		if(mblNoHmac == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNoHmac");
		
		return new MblUniqueCheckQuery(
				mblNoHmac
		); 
	}
}
