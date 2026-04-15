package com.prafta.web.chkLst.chkLst03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst03.dto.request.InspectResultRequest;

public record InspectResultParam(
	String fromDate				// 점검조회 시작 월
	, String toDate					// 점검조회 종료 월
	, String siteCd					// 사업장코드
	, String chkptNm				// 점검대상명칭
	, String chkLstType				// 일일점검구분
	, String gvCmpnyCd
	, String gvUserCd
){
	public static InspectResultParam from(InspectResultRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - InspectResultRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        return new InspectResultParam(
    		request.getFromDate()
    		, request.getToDate()
    		, request.getSiteCd()
    		, request.getChkptNm()
    		, request.getChkLstType()
    		, tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
        );
    }
}
