package com.prafta.web.tbm.tbm01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduInfoListRequest;

public record TbmEduInfoListParam(
	String mtrlCd
	, String mtrlType
	, String title
	, String useYn
	, String gvCmpnyCd
){
	public static TbmEduInfoListParam from(TbmEduInfoListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduInfoListRequest");
		if(tokenInfo == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_003,"\n필수값 누락 - TokenInfo");
		
		return new TbmEduInfoListParam(
			request.getMtrlCd()
			, request.getMtrlType()
			, request.getTitle()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}
