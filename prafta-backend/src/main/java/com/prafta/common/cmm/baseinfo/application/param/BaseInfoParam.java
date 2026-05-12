package com.prafta.common.cmm.baseinfo.application.param;

import com.prafta.common.cmm.baseinfo.dto.request.BaseInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record BaseInfoParam (
	String cmpnyCd
	, String codeD
	, String nameD
	, String code
) {
	public static BaseInfoParam from(BaseInfoRequest request, TokenInfo tokenInfo) {

		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - BaseInfoRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_003,"\n필수값 누락 - TokenInfo");
		
		return new BaseInfoParam(
				tokenInfo.gv_cmpnyCd()
			, request.getCodeD()
			, request.getNameD()
			, request.getCode()
		); 
	}
}
