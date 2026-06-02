package com.prafta.web.risk.risk03.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.web.risk.risk03.application.param.AssessmentParam;
import com.prafta.web.risk.risk03.application.param.RiskAssessmentsListParam;
import com.prafta.web.risk.risk03.application.param.RiskTypeInfoListParam;
import com.prafta.web.risk.risk03.dto.response.RiskAssessmentsListResponse;
import com.prafta.web.risk.risk03.dto.response.RiskTypeListResponse;

public interface Risk03Service {
	
	RiskTypeListResponse selectRiskTypeInfoList(RiskTypeInfoListParam param);
	
	RiskAssessmentsListResponse selectRiskAssessmentsLists(RiskAssessmentsListParam param);
	
	void saveAssessment(AssessmentParam param, MultipartFile file);
}
