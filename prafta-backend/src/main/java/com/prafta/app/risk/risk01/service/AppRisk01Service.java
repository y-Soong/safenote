package com.prafta.app.risk.risk01.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.risk.risk01.dto.RiskAssessmentReq;
import com.prafta.app.risk.risk01.dto.RiskInfoReq;
import com.prafta.app.risk.risk01.dto.RiskInfoRes;
import com.prafta.common.dto.TokenInfo;

public interface AppRisk01Service {

	RiskInfoRes selectRiskTypeInfo(RiskInfoReq request, TokenInfo tokenInfo);

	void saveRiskAssessments(RiskAssessmentReq request, MultipartFile file, TokenInfo tokenInfo);
	
//	ChecklistInfoRes selectChkLstInfo(ChecklistInfoReq request, Map<String, Object> tokenInfo);
//	
//	void saveInspectResult(SaveInspectResultReq request, Map<String, MultipartFile> file, Map<String, Object> tokenInfo);
}
