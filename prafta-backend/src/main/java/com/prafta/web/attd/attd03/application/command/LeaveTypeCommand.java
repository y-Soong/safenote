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
    , Integer grantDays
    , String adminAvailTermType
    , String adminAvailFromDt
    , String adminAvailToDt
    , String grantBaseType
    , Integer grantOffsetMonth
    , String aprvUseYn
    , Integer aprvStepCnt
    , String hrFinalAprvYn
    , String evidenceYn
    , String evidenceGuideMsg
){
	public static LeaveTypeCommand from(LeaveTypeParam param) {
		
		if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - LeaveTypeParam");
		
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
			, param.grantDays()
			, param.adminAvailTermType()
			, param.adminAvailFromDt()
			, param.adminAvailToDt()
			, param.grantBaseType()
			, param.grantOffsetMonth()
			, param.aprvUseYn()
			, param.aprvStepCnt()
			, param.hrFinalAprvYn()
			, param.evidenceYn()
			, param.evidenceGuideMsg()
		);
	}
}
