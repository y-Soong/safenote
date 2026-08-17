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
	// 하위 항목 siteCd 는 게이트 통과한 부모 값으로 서버 강제(SEC-1) — 요청 항목의 siteCd 는 신뢰하지 않는다.
	public static ShiftTeamCommand from(ShiftTeamParam param, String shiftCd, String siteCd, String gvCmpnyCd, String gvUserCd) {

        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (shiftCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (siteCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (gvCmpnyCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (gvUserCd == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftTeamCommand(
    		shiftCd
    		, siteCd
    		, param.teamIdx()
    		, param.teamNm()
    		, gvCmpnyCd
    		, gvUserCd
        );
	}
}
