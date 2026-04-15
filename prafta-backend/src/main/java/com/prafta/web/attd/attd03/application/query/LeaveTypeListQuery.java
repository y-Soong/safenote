package com.prafta.web.attd.attd03.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.application.param.LeaveTypeListParam;

public record LeaveTypeListQuery(
	String leaveNo
	, String leaveNm
	, String leaveType
	, String useYn
	, String gvCmpnyCd
){
	public static LeaveTypeListQuery from(LeaveTypeListParam param) {
		
		if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - LeaveTypeListParam");
		
		return new LeaveTypeListQuery(
			param.leaveNo()
			, param.leaveNm()
			, param.leaveType()
			, param.useYn()
			, param.gvCmpnyCd()
		);
	}
}
