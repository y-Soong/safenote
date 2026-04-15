package com.prafta.web.attd.attd01.result;

public record SchHistResult(
	String cmpnyCd
	, String siteCd
	, String schCd
	, String applyDate

	, String fstSchTime	
	, String fstSchBrkMin

	, String secSchTime
	, String secSchBrkMin
	, String useYn
	
	, String userId
	, String userNm
	, String insertDate
){

}
