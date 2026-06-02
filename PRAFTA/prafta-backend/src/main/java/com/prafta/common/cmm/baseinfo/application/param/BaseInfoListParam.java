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
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new BaseInfoListParam(
				request.getBaseCodeList()
				, tokenInfo.gv_cmpnyCd()
		);
	}
}
