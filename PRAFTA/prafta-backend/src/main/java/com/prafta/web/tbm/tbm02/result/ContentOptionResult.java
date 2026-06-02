package com.prafta.web.tbm.tbm02.result;

/** 콘텐츠 선택 모달 옵션(콘텐츠 묶음 단위). */
public record ContentOptionResult(
	String mtrlCd
	, String title
	, String mtrlType			// 카테고리(COM003)
	, String mtrlTypeNm
	, String siteCd
	, String isCommonContent	// 회사공통 여부(Y/N)
	, int itemCnt				// 묶음 내 세부항목 수
){

}
