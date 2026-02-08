package com.prafta.web.risk.risk01.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.risk.risk01.dto.RiskHazardListReq;
import com.prafta.web.risk.risk01.dto.RiskHazardListRes;
import com.prafta.web.risk.risk01.dto.RiskHazardReq;
import com.prafta.web.risk.risk01.dto.RiskTypeListReq;
import com.prafta.web.risk.risk01.dto.RiskTypeListRes;
import com.prafta.web.risk.risk01.dto.RiskTypeReq;

public interface Risk01Service {
	
	RiskTypeListRes selectRiskTypeList(RiskTypeListReq dto, Map<String, Object> tokenInfo);
	
	void updateRistType(List<RiskTypeReq> dtoList, Map<String, Object> tokenInfo);
	
	void deleteRistType(List<RiskTypeReq> dtoList, Map<String, Object> tokenInfo);
	
	RiskHazardListRes selectRiskHazardList(RiskHazardListReq dto, Map<String, Object> tokenInfo);
	
	void updateRiskHazard(List<RiskHazardReq> dtoList, Map<String, Object> tokenInfo);
	
	void deleteRiskHazard(List<RiskHazardReq> dtoList, Map<String, Object> tokenInfo);
}
