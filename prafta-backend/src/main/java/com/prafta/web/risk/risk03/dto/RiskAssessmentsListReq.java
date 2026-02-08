package com.prafta.web.risk.risk03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskAssessmentsListReq{
	String siteCd;
	String assessmentStatus;
	String processCd;
	String riskTypeCd;
}
