package com.prafta.app.risk.risk01.dto;

import lombok.Data;

@Data
public class RiskAssessmentReq {
	String siteCd;
    String processCd;
    String riskTypeCd;
    String hazardCd;
    String assessmentDesc;   
    String initLikelihoodScore;
    String initSeverityScore;
    String initRiskLv;
    String initDesc;
}
