package com.prafta.web.risk.risk03.dto.response;

import java.util.List;

import com.prafta.web.risk.risk03.result.RiskAssessmentResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskAssessmentsListResponse{
	private List<RiskAssessmentResult> riskAssessmentResultList;
}
