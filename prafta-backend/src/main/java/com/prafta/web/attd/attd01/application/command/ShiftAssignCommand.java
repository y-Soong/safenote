package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftAssignParam;

public record ShiftAssignCommand(
	String shiftCd
	, String siteCd
	, String dayNo
	, String teamIdx
	, String assignYn
	, String schCd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ShiftAssignCommand from(ShiftAssignParam param, String shiftCd, String gvCmpnyCd, String gvUserCd) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftAssignParam");
        if (shiftCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - shiftCd");
        if (gvCmpnyCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - gvCmpnyCd");
        if (gvUserCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - gvUserCd");

        return new ShiftAssignCommand(
    		shiftCd
    		, param.siteCd()
    		, param.dayNo()
    		, param.teamIdx()
    		, param.assignYn()
    		, param.schCd()
    		, gvCmpnyCd
    		, gvUserCd
        );
	}
}
