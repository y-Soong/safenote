package com.prafta.web.attd.attd01.result;

public record SchHistResult(
	String cmpnyCd
	, String siteCd
	, String schCd
	, String applyDate

	, String fstSchTime	
	, String fstSchBrkMin
	, String fstBrkStrTime
	, String fstBrkEndTime

	, String secSchTime
	, String secSchBrkMin
	, String secBrkStrTime
	, String secBrkEndTime
	, String useYn
	
	, String userId
	, String userNm
	, String insertDate

	// PRAFTA-FIXEDOT-1: 전방·후방 고정연장근무 FROM/TO (HHMM, NULL=고정연장 없음).
	// ★selectSchHistList SELECT 말미 append 순서와 반드시 일치(위치기반 record 매핑).
	, String preFixedOtStrTime
	, String preFixedOtEndTime
	, String fixedOtStrTime
	, String fixedOtEndTime
){

}
