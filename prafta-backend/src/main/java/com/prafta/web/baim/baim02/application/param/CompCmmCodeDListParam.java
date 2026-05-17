package com.prafta.web.baim.baim02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim02.dto.request.CompCmmCodeDListRequest;

public record CompCmmCodeDListParam(
		String codeCd
		, String gvCmpnyCd
	) {
		public static CompCmmCodeDListParam from(CompCmmCodeDListRequest request, TokenInfo tokenInfo) {
			
			if(request == null)
				throw new ApiException(CommonErrorCode.COMMON_400_001);
			if(tokenInfo == null)
				throw new ApiException(CommonErrorCode.COMMON_400_003);
			
			return new CompCmmCodeDListParam(
				request.getCodeCd()
				,tokenInfo.gv_cmpnyCd()
			); 
		}
	}
