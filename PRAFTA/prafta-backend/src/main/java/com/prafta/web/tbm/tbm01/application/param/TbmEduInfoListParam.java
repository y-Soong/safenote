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
	, String siteCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static TbmEduInfoListParam from(TbmEduInfoListRequest request, TokenInfo tokenInfo) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new TbmEduInfoListParam(
			request.getMtrlCd()
			, request.getMtrlType()
			, request.getTitle()
			, request.getUseYn()
			, request.getSiteCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
