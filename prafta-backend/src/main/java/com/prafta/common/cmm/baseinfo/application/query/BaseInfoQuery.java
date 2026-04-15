package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.BaseInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record BaseInfoQuery (
	String cmpnyCd
	, String codeD
	, String nameD
	, String code
){
	public static BaseInfoQuery from(BaseInfoParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - BaseInfoParam");
		
		return new BaseInfoQuery(
			param.cmpnyCd()
			, param.codeD()
			, param.nameD()
			, param.code()
		); 
	}
}
