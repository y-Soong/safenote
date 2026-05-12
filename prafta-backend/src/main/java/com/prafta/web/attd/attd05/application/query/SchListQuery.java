package com.prafta.web.attd.attd05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;


public record SchListQuery (
	String siteCd
	, String gvCmpnyCd
){
	public static SchListQuery from(SchTypeListParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchListParam");

        return new SchListQuery(
    		param.siteCd()
    		, param.gvCmpnyCd()
		);
    }
}
