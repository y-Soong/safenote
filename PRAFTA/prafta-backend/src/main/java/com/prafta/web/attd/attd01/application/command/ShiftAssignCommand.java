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
	// 하위 항목 siteCd 는 게이트 통과한 부모 값으로 서버 강제(SEC-1) — 요청 항목의 siteCd 는 신뢰하지 않는다.
	public static ShiftAssignCommand from(ShiftAssignParam param, String shiftCd, String siteCd, String gvCmpnyCd, String gvUserCd) {

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

        return new ShiftAssignCommand(
    		shiftCd
    		, siteCd
    		, param.dayNo()
    		, param.teamIdx()
    		, param.assignYn()
    		, param.schCd()
    		, gvCmpnyCd
    		, gvUserCd
        );
	}
}
