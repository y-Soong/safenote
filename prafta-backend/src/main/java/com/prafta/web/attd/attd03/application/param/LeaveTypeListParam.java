package com.prafta.web.attd.attd03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.dto.request.LeaveTypeListRequest;

public record LeaveTypeListParam(
	String leaveNo
	, String leaveNm
	, String leaveType
	, String useYn
	, String gvCmpnyCd
){
	public static LeaveTypeListParam from(LeaveTypeListRequest request, TokenInfo tokenInfo) {
		
		if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - LeaveTypeListRequest");
		
		return new LeaveTypeListParam(
			request.getLeaveNo()
			, request.getLeaveNm()
			, request.getLeaveType()
			, request.getUseYn()
			, tokenInfo.gv_cmpnyCd()
		);
	}
}
