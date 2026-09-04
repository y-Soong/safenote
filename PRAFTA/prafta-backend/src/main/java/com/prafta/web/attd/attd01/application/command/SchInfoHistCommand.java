package com.prafta.web.attd.attd01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;

public record SchInfoHistCommand(
	String cmpnyCd
	, String siteCd
	, int histIdx
	, String schCd
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

	// PRAFTA-FIXEDOT-1: 전방·후방 고정연장근무 FROM/TO (HHMM, 선택) — 이력 스냅샷에 동반 기록(V8)
	, String preFixedOtStrTime
	, String preFixedOtEndTime
	, String fixedOtStrTime
	, String fixedOtEndTime

	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SchInfoHistCommand from(SchInfoParam param, int histIdx, String schCd) {
		
        if (param == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SchInfoHistCommand(
    		param.cmpnyCd()
    		, param.siteCd()
    		, histIdx
    		, schCd
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

	/** BW-10(G-6): 휴게 종료 파생값을 이력 스냅샷에도 동일하게 반영한 사본(현행/이력 짝 규약). */
	public SchInfoHistCommand withBreakEndTimes(String newFstBrkEndTime, String newSecBrkEndTime) {
		return new SchInfoHistCommand(
			cmpnyCd, siteCd, histIdx, schCd, applyDate
			, fstSchStrTime, fstSchEndTime, fstSchBrkMin, fstBrkStrTime, newFstBrkEndTime
			, secSchStrTime, secSchEndTime, secSchBrkMin, secBrkStrTime, newSecBrkEndTime
			, preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime
			, useYn, gvCmpnyCd, gvUserCd);
	}
}
