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
	// ★record 매핑은 SELECT 컬럼 순서와 일치해야 하므로 반드시 말미 유지.
	, String linkSrcCmpnyCd
){

}
