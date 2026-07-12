package com.prafta.web.tbm.tbm02.result;

/** 세션 상세 - 연계 콘텐츠 묶음 매핑 + 묶음 정보. */
public record SessionContentResult(
	String mtrlCd
	, String title				// 묶음 제목(TB_TBM_EDU_MTRL.TITLE)
	, String mtrlType			// 카테고리(COM003)
	, String mtrlTypeNm
	, int itemCnt				// 묶음 내 세부항목 수
	, int displayOrder
	, String overrideDesc		// 세션별 override 설명
	, String siteCd				// 교육자료 스코프(NULL=회사공통)
	, String isCommonContent	// 'Y'=회사공통, 'N'=특정 사업장(6.2-(1)-2 스코프 표시)
){

}
