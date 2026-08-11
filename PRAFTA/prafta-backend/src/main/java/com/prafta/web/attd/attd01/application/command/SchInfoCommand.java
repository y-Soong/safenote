package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;

public record SchInfoCommand(
	String cmpnyCd
	, String siteCd
	, String schCd
	, String schNo
	, String schType
	, String applyDate
	
	, String fstSchStrTime
	, String fstSchEndTime
	, String fstSchBrkMin
	, String fstBrkStrTime
	, String fstBrkEndTime

	, String secSchStrTime
	, String secSchEndTime
	, String secSchBrkMin
	, String secBrkStrTime
	, String secBrkEndTime

	// PRAFTA-FIXEDOT-1: 전방·후방 고정연장근무 FROM/TO (HHMM, 선택)
	, String preFixedOtStrTime
	, String preFixedOtEndTime
	, String fixedOtStrTime
	, String fixedOtEndTime

	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SchInfoCommand from(SchInfoParam param, String schCd) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoCommand(
    		param.cmpnyCd()
    		, param.siteCd()
    		, schCd
    		, param.schNo()
    		, param.schType()
    		, param.applyDate()
    		, param.fstSchStrTime()
    		, param.fstSchEndTime()
    		, param.fstSchBrkMin()
    		, param.fstBrkStrTime()
    		, param.fstBrkEndTime()
    		, param.secSchStrTime()
    		, param.secSchEndTime()
    		, param.secSchBrkMin()
    		, param.secBrkStrTime()
    		, param.secBrkEndTime()
    		, param.preFixedOtStrTime()
    		, param.preFixedOtEndTime()
    		, param.fixedOtStrTime()
    		, param.fixedOtEndTime()
    		, param.useYn()
    		, param.gvCmpnyCd()
    		, param.gvUserCd()
        );
	}
}
