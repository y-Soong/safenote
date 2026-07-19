package com.prafta.web.chkLst.chkLst02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;

public record ChkptInspectItemListQuery(
	String siteCd
	, String codeCd
	, String gvCmpnyCd
){
    public static ChkptInspectItemListQuery from(ChkptInspectItemListParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptInspectItemListQuery(
    		param.siteCd()
    		, param.codeCd()
    		, param.gvCmpnyCd()
        );
    }
}	
