package com.prafta.web.attd.attd05.application.query;

import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;

public record LeaveTypeListQuery (
	String gvCmpnyCd
	// prafta-com-011-6 가불 메타: 잔여(balanceDays) 산정 입력(토큰 도출 userCd + 당해 회계연도 윈도우).
	, String gvUserCd
	, String fiscalStartYmd
	, String fiscalEndYmdExclusive
){
	public static LeaveTypeListQuery from(LeaveTypeListParam param,
			String fiscalStartYmd, String fiscalEndYmdExclusive) {

        return new LeaveTypeListQuery(
        		param.gvCmpnyCd()
        		, param.gvUserCd()
        		, fiscalStartYmd
        		, fiscalEndYmdExclusive
		);
    }
}
