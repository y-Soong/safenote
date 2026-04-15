package com.prafta.web.risk.risk01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.risk.risk01.application.command.RiskHazardCommand;
import com.prafta.web.risk.risk01.application.command.RiskTypeCommand;
import com.prafta.web.risk.risk01.application.query.RiskHazardCountQuery;
import com.prafta.web.risk.risk01.application.query.RiskHazardListQuery;
import com.prafta.web.risk.risk01.application.query.RiskTypeListQuery;
import com.prafta.web.risk.risk01.result.RiskHazardResult;
import com.prafta.web.risk.risk01.result.RiskTypeResult;

@Mapper
public interface Risk01Mapper {
	
	List<RiskTypeResult> selectRiskTypeList(RiskTypeListQuery query);
	
	void mergeRistType(RiskTypeCommand command);
	
	int selectRiskHazardCnt(RiskHazardCountQuery query);
	
	void deleteRistType(RiskTypeCommand command);
	
	List<RiskHazardResult> selectRiskHazardList(RiskHazardListQuery query);
	
	void mergeRiskHazard(RiskHazardCommand command);
	
	void deleteRiskHazard(RiskHazardCommand command);
}
