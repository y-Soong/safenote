package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam;

public record ShiftTypeCommand(
	String siteCd
	, String shiftCd
	, String shiftNo
	, String shiftPtrnCnt
	, String shiftTeamCnt
	, String shiftCycleDays
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ShiftTypeCommand from(ShiftSchInfoParam param, String shiftCd) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftSchInfoParam");
        if (shiftCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - shiftCd");

        return new ShiftTypeCommand(
    		param.shiftType().siteCd()
    		, shiftCd
    		, param.shiftType().shiftNo()
    		, param.shiftType().shiftPtrnCnt()
    		, param.shiftType().shiftTeamCnt()
    		, param.shiftType().shiftCycleDays()
    		, param.shiftType().useYn()
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );
	}
}
