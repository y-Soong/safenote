package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserIdInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record UserIdInfoQuery(
	String userNm
	, String mblNoEnc
) {
	public static UserIdInfoQuery from(UserIdInfoParam param, String mblNoEnc) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserIdInfoParam");
		if(mblNoEnc == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mblNoEnc");
		
		return new UserIdInfoQuery(
			param.userNm()
			, mblNoEnc
		); 
	}
}
