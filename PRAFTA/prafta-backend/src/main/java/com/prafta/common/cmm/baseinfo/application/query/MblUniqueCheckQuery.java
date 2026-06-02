package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record MblUniqueCheckQuery(
		String mblNoHmac     // 사용자가 입력한 번호 (원본)
) {
	public static MblUniqueCheckQuery from(String mblNoHmac) {
		
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new MblUniqueCheckQuery(
				mblNoHmac
		); 
	}
}
