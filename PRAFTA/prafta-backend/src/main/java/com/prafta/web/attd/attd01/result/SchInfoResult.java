package com.prafta.web.attd.attd01.result;

public record SchInfoResult(
	String cmpnyCd
	, String siteCd
	, String schCd
	, String schNo
	, String schType
	, String schTypeNm
	, String applyDate
	, String fstSchStrTime
	, String fstSchEndTime
	, String fstSchTime
	, String fstSchBrkMin
	, String fstBrkStrTime
	, String fstBrkEndTime
	, String secSchStrTime
	, String secSchEndTime
	, String secSchTime
	, String secSchBrkMin
	, String secBrkStrTime
	, String secBrkEndTime
	, String useYn
	, String useYnNm
	// PRAFTA-SUBCON-T2-04: 연동 원본 회사코드(NULL=일반, NOT NULL=미러 — 편집 비활성 근거).
	// ★record 매핑은 SELECT 컬럼 순서와 일치해야 하므로 SELECT append 순서와 동일하게 유지.
	, String linkSrcCmpnyCd
	// PRAFTA-FIXEDOT-1: 전방·후방 고정연장근무 FROM/TO (HHMM, NULL=고정연장 없음).
	// ★selectSchInfoList SELECT 말미 append 순서와 반드시 일치(위치기반 record 매핑).
	, String preFixedOtStrTime
	, String preFixedOtEndTime
	, String fixedOtStrTime
	, String fixedOtEndTime
){

}
