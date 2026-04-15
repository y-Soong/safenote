package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoListParam;

public record SchInfoListQuery(
	String siteCd
	, String schNo
	, String schType
	, String useYn
	, String gvCmpnyCd
){
	public static SchInfoListQuery from(SchInfoListParam param) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchInfoListQuery");

        return new SchInfoListQuery(
    		param.siteCd()
    		, param.schNo()
    		, param.schType()
    		, param.useYn()
    		, param.gvCmpnyCd()
        );
	}
}
