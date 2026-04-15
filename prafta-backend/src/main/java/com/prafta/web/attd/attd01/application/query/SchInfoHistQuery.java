package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoHistParam;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;


public record SchInfoHistQuery(
	String siteCd
	, String schCd
	, String gvCmpnyCd
){
	public static SchInfoHistQuery from(SchInfoParam param) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchInfoParam");

        return new SchInfoHistQuery(
    		param.siteCd()
    		, param.schCd()
    		, param.gvCmpnyCd()
        );
	}
	
	public static SchInfoHistQuery from(SchInfoHistParam param) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchInfoHistParam");

        return new SchInfoHistQuery(
    		param.siteCd()
    		, param.schCd()
    		, param.gvCmpnyCd()
        );
	}
}
