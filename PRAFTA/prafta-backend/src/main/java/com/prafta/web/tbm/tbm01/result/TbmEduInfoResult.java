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

	, String lockedYn			// T5-2: 사용 중(취소 외 세션 참조) 여부(Y/N). 'Y'면 수정/삭제 잠금
	, String genContent			// TBM_AI F1: GEN_CONTENT(생성 교육안, text)
	, String genAt				// TBM_AI F1: GEN_AT(교육안 생성일시, DATE_FORMAT 문자열)
){

}
