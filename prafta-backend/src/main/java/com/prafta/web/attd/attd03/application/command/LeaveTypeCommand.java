package com.prafta.web.attd.attd03.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;

public record LeaveTypeCommand(
	String leaveCd
    , String leaveType
    , String grantType
    , String leaveNo
    , String leaveNm
    , String paidType
    , String leaveNatureType
    , String useYn
    , String leaveDesc
    , Integer maxAplyDays
    , String useUnitType
    , String availTermType
    , String availFromDt
    , String availToDt
    , String adminAvailTermType
    , String adminAvailFromDt
    , String adminAvailToDt
    , String grantBaseType
    , Integer grantOffsetMonth
    , String grantAssignMmdd
    , String aprvUseYn
    , String evidenceYn
    , String evidenceGuideMsg
){
	public static LeaveTypeCommand from(LeaveTypeParam param) {
		
		if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new LeaveTypeCommand(
			param.leaveCd()
			, param.leaveType()
			, param.grantType()
			, param.leaveNo()
			, param.leaveNm()
			, param.paidType()
			, param.leaveNatureType()
			, param.useYn()
			, param.leaveDesc()
			, param.maxAplyDays()
			, param.useUnitType()
			, param.availTermType()
			, param.availFromDt()
			, param.availToDt()
			, param.adminAvailTermType()
			, param.adminAvailFromDt()
			, param.adminAvailToDt()
			, param.grantBaseType()
			, param.grantOffsetMonth()
			, param.grantAssignMmdd()
			, param.aprvUseYn()
			, param.evidenceYn()
			, param.evidenceGuideMsg()
		);
	}
}
