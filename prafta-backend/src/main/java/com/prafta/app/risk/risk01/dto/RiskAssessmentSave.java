package com.prafta.app.risk.risk01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskAssessmentSave {
	String siteCd;
    String processCd;
    String riskTypeCd;
    String hazardCd;
    String assessmentDesc;
    String assessmentStatus;
    String initLikelihoodScore;
    String initSeverityScore;
    String initRiskLv;
    String initDesc;
    String initFileMgmtCd;
}
