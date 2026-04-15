package com.prafta.web.risk.risk03.result;

public record RiskAssessmentResult(
	String cmpnyCd
	, String siteCd
	, String processCd
	, String processNm
	, String riskTypeCd
	, String riskTypeNm
	, String hazardCd
	, String hazardNm
	, String assessmentCd
	, String assessmentStatus
	, String assessmentStatusNm
	, String initLikelihoodScore
	, String initSeverityScore
	, String initRiskLv
	, String initDesc
	, String initAssessorId
	, String initAssessorNm
	, String initAssessDate
	, String initFileMgmtCd
	, String initFilePath
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
