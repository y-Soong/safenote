package com.prafta.web.tbm.tbm01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduDetailRequest;

/**
 * prafta-033-A: W-03 상세 조회 Param.
 * 권한/스코프 판정에 필요한 토큰 클레임(사업장/권한)을 함께 보유한다.
 */
public record TbmEduDetailParam(
	String mtrlCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
){
	public static TbmEduDetailParam from(TbmEduDetailRequest request, TokenInfo tokenInfo) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		if(request.getMtrlCd() == null || request.getMtrlCd().isEmpty())
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new TbmEduDetailParam(
			request.getMtrlCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
		);
	}
}
