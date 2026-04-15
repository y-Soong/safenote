package com.prafta.web.baim.baim02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeMListParam;

public record CompCmmCodeMListQuery(
	String codeCd
	, String codeNm
	, String gvCmpnyCd
) {
public static CompCmmCodeMListQuery from(CompCmmCodeMListParam param) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - CompCmmCodeMListParam");
		
		return new CompCmmCodeMListQuery(
			param.codeCd()
			, param.codeNm()
			,param.gvCmpnyCd()
		); 
	}
}
