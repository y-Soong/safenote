package com.prafta.common.cmm.baseinfo.application.param;

import java.util.List;

import com.prafta.common.cmm.baseinfo.dto.request.BaseInfoListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record BaseInfoListParam (
	List<String> baseCodeList
	, String cmpnyCd
){
	public static BaseInfoListParam from(BaseInfoListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - BaseInfoListRequest");
		
		return new BaseInfoListParam(
				request.getBaseCodeList()
				, tokenInfo != null ? tokenInfo.gv_cmpnyCd() : request.getCmpnyCd()
		);
	}
}
