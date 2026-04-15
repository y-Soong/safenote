package com.prafta.web.chkLst.chkLst03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst03.dto.request.InspectResultDetailRequest;

public record InspectResultDetailParam(
	String workMonth		// 조회 기준 월
	, String siteCd			// 사업장코드
	, String chkLstType		// 체크리스트 타입
	, String chkptCd		// 점검항목코드
	, String gvCmpnyCd
) {
	public static InspectResultDetailParam from(InspectResultDetailRequest request, TokenInfo tokenInfo) {

        if (request == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - InspectResultDetailRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        return new InspectResultDetailParam(
    		request.getWorkMonth()
    		, request.getSiteCd()
    		, request.getChkLstType()
    		, request.getChkptCd()
    		, tokenInfo.gv_cmpnyCd()
        );
    }
}
