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
){

}
