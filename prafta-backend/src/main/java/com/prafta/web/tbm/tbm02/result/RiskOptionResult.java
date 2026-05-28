package com.prafta.web.tbm.tbm02.result;

/**
 * 위험성평가 선택 모달 옵션.
 *
 * <p>표시명은 Risk03 규약(공정명/위험요인구분명/유해요인명)을 재사용해 구성한다.
 */
public record RiskOptionResult(
	String siteCd
	, String processCd
	, String processNm
	, String riskTypeCd
	, String riskTypeNm
	, String hazardCd
	, String hazardNm
	, String assessmentCd
	, String assessmentStatus
	, String assessmentStatusNm
){

}
