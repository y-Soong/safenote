package com.prafta.app.risk.risk01.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.risk.risk01.dto.RiskAssessmentReq;
import com.prafta.app.risk.risk01.dto.RiskInfoReq;
import com.prafta.app.risk.risk01.dto.RiskInfoRes;

public interface AppRisk01Service {
	
	RiskInfoRes selectRiskTypeInfo(RiskInfoReq request, Map<String, Object> tokenInfo);
	
	void saveRiskAssessments(RiskAssessmentReq request, MultipartFile file, Map<String, Object> tokenInfo);
	
//	ChecklistInfoRes selectChkLstInfo(ChecklistInfoReq request, Map<String, Object> tokenInfo);
//	
//	void saveInspectResult(SaveInspectResultReq request, Map<String, MultipartFile> file, Map<String, Object> tokenInfo);
}
