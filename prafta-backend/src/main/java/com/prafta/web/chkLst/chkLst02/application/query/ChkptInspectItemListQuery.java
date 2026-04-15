package com.prafta.web.chkLst.chkLst02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;

public record ChkptInspectItemListQuery(
	String codeCd
	, String gvCmpnyCd
){
    public static ChkptInspectItemListQuery from(ChkptInspectItemListParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ChkptInspectItemListParam");

        return new ChkptInspectItemListQuery(
    		param.codeCd()
    		, param.gvCmpnyCd()
        );
    }
}	
