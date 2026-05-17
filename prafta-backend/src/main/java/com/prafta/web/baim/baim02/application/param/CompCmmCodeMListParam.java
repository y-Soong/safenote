package com.prafta.web.baim.baim02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim02.dto.request.CompCmmCodeMListRequest;

public record CompCmmCodeMListParam(
	String codeCd
	, String codeNm
	, String gvCmpnyCd
) {
	public static CompCmmCodeMListParam from(CompCmmCodeMListRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		
		return new CompCmmCodeMListParam(
			request.getCodeCd()
			, request.getCodeNm()
			,tokenInfo.gv_cmpnyCd()
		); 
	}
}
