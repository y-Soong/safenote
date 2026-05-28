package com.prafta.web.tbm.tbm01.result;

public record TbmEduInfoResult(
	String cmpnyCd
	, String mtrlCd
	, String title
	, String contents
	, String mtrlType
	, String useYn
	, String mtrlCnt

	, String siteCd				// prafta-033-A: 스코프(사업장코드). NULL=회사공통
	, String isCommonContent	// prafta-033-A: 회사공통 여부(Y/N, SITE_CD IS NULL 산출)

	, String oriTitle
	, String oriContents
	, String oriMtrlType
	, String oriUseYn

	, String insertNm
	, String insertDate
){

}
