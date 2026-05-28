package com.prafta.web.tbm.tbm02.result;

/**
 * 세션 상세 - 연계 위험성평가 매핑 + 표시명.
 *
 * <p>TB_RISK_ASSESSMENT에 TITLE 컬럼이 없으므로(plan §8-1) 위험성평가 모듈(Risk03)의
 * 표시 규약을 재사용한다: 공정명(COM002) / 위험요인구분명 / 유해요인명을 조인해 구성하고,
 * displayName 으로 합성한다.
 */
public record SessionRiskResult(
	String siteCd
	, String processCd
	, String processNm			// 공정명(COM002)
	, String riskTypeCd
	, String riskTypeNm			// 위험요인구분명(TB_RISK_TYPE)
	, String hazardCd
	, String hazardNm			// 유해요인명(TB_RISK_SITE_HAZARD, self=직접입력)
	, String assessmentCd
	, String assessmentStatus
	, String assessmentStatusNm
	, int displayOrder
){

}
