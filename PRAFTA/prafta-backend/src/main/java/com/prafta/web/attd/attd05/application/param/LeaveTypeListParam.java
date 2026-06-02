package com.prafta.web.attd.attd05.application.param;

import com.prafta.common.dto.TokenInfo;


public record LeaveTypeListParam (
	String gvCmpnyCd
){
	public static LeaveTypeListParam from(TokenInfo tokenInfo) {

        return new LeaveTypeListParam(
    		tokenInfo.gv_cmpnyCd()
		);
    }
}
