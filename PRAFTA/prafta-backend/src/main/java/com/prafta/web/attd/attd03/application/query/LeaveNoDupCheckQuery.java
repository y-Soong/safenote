package com.prafta.web.attd.attd03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;

public record LeaveNoDupCheckQuery(
	String leaveNo
	, String gvCmpnyCd
){
	public static LeaveNoDupCheckQuery from(LeaveTypeParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        return new LeaveNoDupCheckQuery(
        	param.leaveNo()
        	, param.gvCmpnyCd()
        );
	}
	
}
