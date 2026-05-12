package com.prafta.web.chkLst.chkLst01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst01.dto.request.ChkptListRequest;

public record ChkptListParam(
	String siteCd
	, String chkptNm
	, String chkLstType
	, String useYn
	, String gvCmpnyCd
){
	public static ChkptListParam from(ChkptListRequest request, TokenInfo tokenInfo) {
		
        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ChkptListRequest");

        return new ChkptListParam(
        	request.getSiteCd()
        	, request.getChkptNm()
        	, request.getChkLstType()
        	, request.getUseYn()
        	, tokenInfo.gv_cmpnyCd()
        );
    }
}
