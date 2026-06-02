package com.prafta.web.chkLst.chkLst02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemListRequest;

public record ChkptInspectItemListParam(
	String codeCd
	, String gvCmpnyCd
){
    public static ChkptInspectItemListParam from(ChkptInspectItemListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChkptInspectItemListParam(
        		request.getCodeCd()
        		, tokenInfo.gv_cmpnyCd()
        );
    }
}
