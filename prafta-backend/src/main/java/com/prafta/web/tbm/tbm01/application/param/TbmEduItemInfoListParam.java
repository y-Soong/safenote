package com.prafta.web.tbm.tbm01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduItemInfoListRequest;

public record TbmEduItemInfoListParam(
	String mtrlCd
	, String gvCmpnyCd
){
	public static TbmEduItemInfoListParam from(TbmEduItemInfoListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		
		return new TbmEduItemInfoListParam(
			request.getMtrlCd()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}