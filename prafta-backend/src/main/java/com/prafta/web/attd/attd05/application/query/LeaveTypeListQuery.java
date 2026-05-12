package com.prafta.web.attd.attd05.application.query;

import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;

public record LeaveTypeListQuery (
	String gvCmpnyCd
){
	public static LeaveTypeListQuery from(LeaveTypeListParam param) {

        return new LeaveTypeListQuery(
        		param.gvCmpnyCd()
		);
    }
}
