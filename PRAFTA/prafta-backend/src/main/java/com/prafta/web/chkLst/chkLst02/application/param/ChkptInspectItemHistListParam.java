package com.prafta.web.chkLst.chkLst02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemHistListRequest;

public record ChkptInspectItemHistListParam(
	String chkLstType
	, String inspectItemCd
	, String gvCmpnyCd
){
    public static ChkptInspectItemHistListParam from(ChkptInspectItemHistListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptInspectItemHistListParam(
        		request.getChkLstType()
        		, request.getInspectItemCd()
        		, tokenInfo.gv_cmpnyCd()
        );
    }
}
