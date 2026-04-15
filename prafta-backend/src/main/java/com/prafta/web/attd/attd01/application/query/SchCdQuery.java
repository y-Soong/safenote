package com.prafta.web.attd.attd01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;


public record SchCdQuery(
	String siteCd
){
	public static SchCdQuery from(SchInfoParam param) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - SchInfoParam");

        return new SchCdQuery(
        		param.siteCd()
        );
	}
}
