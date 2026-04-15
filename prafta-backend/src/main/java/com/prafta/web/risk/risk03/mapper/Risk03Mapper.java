package com.prafta.web.risk.risk03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.risk.risk03.application.command.AssessmentCommand;
import com.prafta.web.risk.risk03.application.query.RiskAssessmentsListQuery;
import com.prafta.web.risk.risk03.application.query.RiskTypeInfoListQuery;
import com.prafta.web.risk.risk03.result.RiskAssessmentResult;
import com.prafta.web.risk.risk03.result.RiskTypeResult;

@Mapper
public interface Risk03Mapper {
	
	List<RiskTypeResult> selectRiskTypeList(RiskTypeInfoListQuery query);
	
	List<RiskAssessmentResult> selectRiskAssessmentsLists(RiskAssessmentsListQuery query);
	
	void updateAssessment(AssessmentCommand command);
}
