package com.prafta.web.chkLst.chkLst02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemHistListRequest;

public record ChkptInspectItemHistListParam(
	String siteCd
	, String chkLstType
	, String inspectItemCd
	, String gvCmpnyCd
	, String gvUserCd
){
    public static ChkptInspectItemHistListParam from(ChkptInspectItemHistListRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // PRAFTA-SUBCON-T0-02: 사업장 키 전환 — siteCd 미전달 시 400
        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
        	throw new ApiException(CommonErrorCode.COMMON_400_001, "사업장코드는 필수입니다.");

        return new ChkptInspectItemHistListParam(
        		request.getSiteCd()
        		, request.getChkLstType()
        		, request.getInspectItemCd()
        		, tokenInfo.gv_cmpnyCd()
        		, tokenInfo.gv_userCd()
        );
    }
}
