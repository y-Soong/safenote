package com.prafta.common.cmm.baseinfo.application.query;

import java.util.List;

import com.prafta.common.cmm.baseinfo.application.param.BaseInfoListParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record BaseInfoListQuery(
		List<String> baseCodeList
		, String cmpnyCd
){
	public static BaseInfoListQuery from(BaseInfoListParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - BaseInfoListParam");
		
		return new BaseInfoListQuery(
				param.baseCodeList()
				, param.cmpnyCd()
		);
	}
}
