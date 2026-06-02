package com.prafta.web.risk.risk03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentsListRequest{
	private String siteCd;
	private String assessmentStatus;
	private String processCd;
	private String riskTypeCd;
}
