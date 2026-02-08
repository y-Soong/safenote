package com.prafta.web.risk.risk03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveAssessmentQry{
	String siteCd;
	String assessmentCd;
	String processCd;
	String assessmentStatus;
	
	String initLikelihoodScore;
	String initSeverityScore;
	String initRiskLv;
	String revalDate;
	String revalBeforeDesc;
	String revalLikelihoodScore;
	String revalSeverityScore;
	String revalRiskLv;
	String revalDesc;
	String revalFileMgmtCd;
}
