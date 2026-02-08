package com.prafta.web.risk.risk03.dto;

import java.util.List;

import com.prafta.web.risk.risk03.vo.RiskAssessment;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskAssessmentsListRes{
	List<RiskAssessment> riskAssessmentList;
}
