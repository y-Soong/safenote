package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftPatternParam;

public record ShiftPatternCommand(
	String shiftCd
	, String siteCd
	, String ptrnIdx
	, String schCd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ShiftPatternCommand from(ShiftPatternParam param, String shiftCd, String gvCmpnyCd, String gvUserCd) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftPatternParam");
        if (shiftCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - shiftCd");
        if (gvCmpnyCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - gvCmpnyCd");
        if (gvUserCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - gvUserCd");

        return new ShiftPatternCommand(
    		shiftCd
    		, param.siteCd()
    		, param.ptrnIdx()
    		, param.schCd()
    		, gvCmpnyCd
    		, gvUserCd
        );
	}
}
