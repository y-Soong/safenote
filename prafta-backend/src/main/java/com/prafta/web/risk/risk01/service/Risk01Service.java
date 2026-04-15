package com.prafta.web.risk.risk01.service;

import com.prafta.web.risk.risk01.application.param.RiskHazardListParam;
import com.prafta.web.risk.risk01.application.param.RiskHazardParam;
import com.prafta.web.risk.risk01.application.param.RiskTypeListParam;
import com.prafta.web.risk.risk01.application.param.RiskTypeParam;
import com.prafta.web.risk.risk01.dto.response.RiskHazardListResponse;
import com.prafta.web.risk.risk01.dto.response.RiskTypeListResponse;

public interface Risk01Service {
	
	RiskTypeListResponse selectRiskTypeList(RiskTypeListParam param);
	
	void updateRistType(RiskTypeParam param);
	
	void deleteRistType(RiskTypeParam param);
	
	RiskHazardListResponse selectRiskHazardList(RiskHazardListParam param);
	
	void updateRiskHazard(RiskHazardParam param);
	
	void deleteRiskHazard(RiskHazardParam param);
}
