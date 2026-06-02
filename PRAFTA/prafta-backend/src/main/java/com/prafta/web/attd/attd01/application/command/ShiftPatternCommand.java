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
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (shiftCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (gvCmpnyCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (gvUserCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

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
