package com.prafta.web.risk.risk03.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.web.risk.risk03.dto.RiskAssessmentsListReq;
import com.prafta.web.risk.risk03.dto.RiskAssessmentsListRes;
import com.prafta.web.risk.risk03.dto.RiskTypeListRes;
import com.prafta.web.risk.risk03.dto.SaveAssessmentReq;

public interface Risk03Service {
	
	RiskTypeListRes selectRiskTypeInfoList(Map<String, Object> tokenInfo);
	
	RiskAssessmentsListRes selectRiskAssessmentsLists(RiskAssessmentsListReq dto, Map<String, Object> tokenInfo);
	
	void saveAssessment(SaveAssessmentReq request, MultipartFile file, Map<String, Object> tokenInfo);
}
