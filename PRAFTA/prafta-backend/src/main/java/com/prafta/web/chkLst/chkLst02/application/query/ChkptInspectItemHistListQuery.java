package com.prafta.web.chkLst.chkLst02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemHistListParam;

public record ChkptInspectItemHistListQuery(
	String siteCd
	, String chkLstType
	, String inspectItemCd
	, String gvCmpnyCd
){
    public static ChkptInspectItemHistListQuery from(ChkptInspectItemHistListParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptInspectItemHistListQuery(
    		param.siteCd()
    		, param.chkLstType()
    		, param.inspectItemCd()
    		, param.gvCmpnyCd()
        );
    }
}
