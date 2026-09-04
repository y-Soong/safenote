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

	/** BW-10(G-6): 휴게 종료 파생값(시작 + 휴게분)을 반영한 사본 — 종료 미전송 시 서버가 채워 저장(FE 파생과 동치). */
	public SchInfoCommand withBreakEndTimes(String newFstBrkEndTime, String newSecBrkEndTime) {
		return new SchInfoCommand(
			cmpnyCd, siteCd, schCd, schNo, schType, applyDate
			, fstSchStrTime, fstSchEndTime, fstSchBrkMin, fstBrkStrTime, newFstBrkEndTime
			, secSchStrTime, secSchEndTime, secSchBrkMin, secBrkStrTime, newSecBrkEndTime
			, preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime
			, useYn, gvCmpnyCd, gvUserCd);
	}
}
