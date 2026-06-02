package com.prafta.web.chkLst.chkLst01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptListParam;

public record ChkptListQuery(
	String siteCd
	, String chkptNm
	, String chkLstType
	, String useYn
	, String gvCmpnyCd
){
	public static ChkptListQuery from(ChkptListParam param) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptListQuery(
    		param.siteCd()
        	, param.chkptNm()
        	, param.chkLstType()
        	, param.useYn()
        	, param.gvCmpnyCd()
        );
    }
}
