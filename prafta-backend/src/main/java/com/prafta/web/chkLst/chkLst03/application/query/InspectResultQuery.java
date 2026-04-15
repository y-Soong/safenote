package com.prafta.web.chkLst.chkLst03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultParam;

public record InspectResultQuery(
	String fromDate				// 점검조회 시작 월
	, String toDate					// 점검조회 종료 월
	, String siteCd					// 사업장코드
	, String chkptNm				// 점검대상명칭
	, String chkLstType				// 일일점검구분
	, String gvCmpnyCd
	, String gvUserCd
){
	public static InspectResultQuery from(InspectResultParam param) {

        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - InspectResultParam");

        return new InspectResultQuery(
    		param.fromDate()
    		, param.toDate()
    		, param.siteCd()
    		, param.chkptNm()
    		, param.chkLstType()
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );
    }
}
