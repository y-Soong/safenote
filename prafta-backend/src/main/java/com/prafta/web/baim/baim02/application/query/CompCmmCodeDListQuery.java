package com.prafta.web.baim.baim02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeDListParam;

public record CompCmmCodeDListQuery(
		String codeCd
		, String gvCmpnyCd
	) {
	public static CompCmmCodeDListQuery from(CompCmmCodeDListParam param) {
			
			if(param == null)
				throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - CompCmmCodeDListParam");
			
			return new CompCmmCodeDListQuery(
				param.codeCd()
				,param.gvCmpnyCd()
			); 
		}
	}
