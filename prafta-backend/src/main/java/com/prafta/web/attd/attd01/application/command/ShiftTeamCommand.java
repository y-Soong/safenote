package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftTeamParam;

public record ShiftTeamCommand(
	String shiftCd
	, String siteCd
	, String teamIdx
	, String teamNm
	, String gvCmpnyCd
	, String gvUserCd
){
	public static ShiftTeamCommand from(ShiftTeamParam param, String shiftCd, String gvCmpnyCd, String gvUserCd) {
		
        if (param == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ShiftTeamParam");
        if (shiftCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - shiftCd");
        if (gvCmpnyCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - gvCmpnyCd");
        if (gvUserCd == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - gvUserCd");

        return new ShiftTeamCommand(
    		shiftCd
    		, param.siteCd()
    		, param.teamIdx()
    		, param.teamNm()
    		, gvCmpnyCd
    		, gvUserCd
        );
	}
}
