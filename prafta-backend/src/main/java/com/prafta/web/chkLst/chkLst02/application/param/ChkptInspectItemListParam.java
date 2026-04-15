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
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ChkptInspectItemListRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        return new ChkptInspectItemListParam(
        		request.getCodeCd()
        		, tokenInfo.gv_cmpnyCd()
        );
    }
}
