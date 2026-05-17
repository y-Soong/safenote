package com.prafta.web.chkLst.chkLst03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultDetailParam;

public record InspectItemSubjQuery(
	String workMonth		// 조회 기준 월
	, String siteCd			// 사업장코드
	, String chkLstType		// 체크리스트 타입
	, String chkptCd		// 점검항목코드
	, String gvCmpnyCd
) {
	public static InspectItemSubjQuery from(InspectResultDetailParam param) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InspectItemSubjQuery(
    		param.workMonth()
    		, param.siteCd()
    		, param.chkLstType()
    		, param.chkptCd()
    		, param.gvCmpnyCd()
        );
    }
}
