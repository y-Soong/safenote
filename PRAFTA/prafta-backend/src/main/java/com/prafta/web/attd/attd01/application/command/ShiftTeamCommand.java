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
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (shiftCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (gvCmpnyCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (gvUserCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

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
