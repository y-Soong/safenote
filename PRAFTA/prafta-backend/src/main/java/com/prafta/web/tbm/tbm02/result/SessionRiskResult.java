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
	// ===== 6.2-(1)-2: 평가요청일/평가요청자(콘솔 요약 표시) =====
	, String cmpnyCd
	, String initAssessDate		// 평가요청일(INIT_ASSESS_DATE)
	, String initAssessorNm		// 평가요청자(INIT_ASSESSOR_ID → USER_NM)
	// ===== 결정#5: 읽기전용 상세 팝업(RiskAssessInfo) 채움용 평가 상세(Risk03 규약 재사용) =====
	, String initLikelihoodScore
	, String initSeverityScore
	, String initRiskLv
	, String initDesc
	, String initAssessorId
	, String initFileMgmtCd		// FNC_CMM_INFO_SRCH FILE_NAME
	, String initFilePath		// FNC_CMM_INFO_SRCH FILE_PATH
	, String revalDate
	, String revalBeforeDesc
	, String revalLikelihoodScore
	, String revalSeverityScore
	, String revalRiskLv
	, String revalDesc
	, String revalAssessorId
	, String revalAssessorNm
	, String revalAssessDate
	, String revalFileMgmtCd
	, String revalFilePath
){

}
