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
	// 평가요청자(이름 부분일치)
	private String initAssessorNm;
	// 평가요청일(단일일 YYYY-MM-DD)
	private String initAssessDate;
}
