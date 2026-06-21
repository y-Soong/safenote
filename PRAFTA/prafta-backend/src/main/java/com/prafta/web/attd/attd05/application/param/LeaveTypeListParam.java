package com.prafta.web.attd.attd05.application.param;

import com.prafta.common.dto.TokenInfo;


public record LeaveTypeListParam (
	String gvCmpnyCd
	// prafta-com-011-6 가불 메타: 잔여(balanceDays)·가불 산정을 토큰 도출 userCd 기준으로 수행(본문 식별값 비신뢰, IDOR 방지).
	, String gvUserCd
){
	public static LeaveTypeListParam from(TokenInfo tokenInfo) {

        return new LeaveTypeListParam(
    		tokenInfo.gv_cmpnyCd()
    		, tokenInfo.gv_userCd()
		);
    }
}
