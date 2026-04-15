package com.prafta.web.risk.risk01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.risk.RiskErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.command.RiskHazardCommand;
import com.prafta.web.risk.risk01.application.command.RiskTypeCommand;
import com.prafta.web.risk.risk01.application.model.RiskHazardModel;
import com.prafta.web.risk.risk01.application.model.RiskTypeModel;
import com.prafta.web.risk.risk01.application.param.RiskHazardListParam;
import com.prafta.web.risk.risk01.application.param.RiskHazardParam;
import com.prafta.web.risk.risk01.application.param.RiskTypeListParam;
import com.prafta.web.risk.risk01.application.param.RiskTypeParam;
import com.prafta.web.risk.risk01.application.query.RiskHazardCountQuery;
import com.prafta.web.risk.risk01.application.query.RiskHazardListQuery;
import com.prafta.web.risk.risk01.application.query.RiskTypeListQuery;
import com.prafta.web.risk.risk01.dto.response.RiskHazardListResponse;
import com.prafta.web.risk.risk01.dto.response.RiskTypeListResponse;
import com.prafta.web.risk.risk01.mapper.Risk01Mapper;
import com.prafta.web.risk.risk01.result.RiskHazardResult;
import com.prafta.web.risk.risk01.result.RiskTypeResult;
import com.prafta.web.risk.risk01.service.Risk01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Risk01ServiceImpl implements Risk01Service{
	private final Risk01Mapper risk01Mapper;
	
	public RiskTypeListResponse selectRiskTypeList(RiskTypeListParam param) {

		RiskTypeListResponse response = null;
		
		List<RiskTypeResult> riskTypeResultList = risk01Mapper.selectRiskTypeList(RiskTypeListQuery.from(param));
		
		if(riskTypeResultList.size() > 0) {
			response = RiskTypeListResponse.builder()
											.riskTypeResultList(riskTypeResultList)
											.build();
		}
		
		return response;
	}

	public void updateRistType(RiskTypeParam param) {
		for(RiskTypeModel model : param.riskTypeModelList()) {
			
			risk01Mapper.mergeRistType(RiskTypeCommand.from(model));
		}
	}
	
	public void deleteRistType(RiskTypeParam param) {
		for(RiskTypeModel model : param.riskTypeModelList()) {
			
			int riskHazardCnt = risk01Mapper.selectRiskHazardCnt(RiskHazardCountQuery.from(model));
			
			if(riskHazardCnt > 0) {
				throw new ApiException(RiskErrorCode.RISK_400_001);
			}
			
			risk01Mapper.deleteRistType(RiskTypeCommand.from(model));
		}
	}
	
	public RiskHazardListResponse selectRiskHazardList(RiskHazardListParam param) {
		
		RiskHazardListResponse response = null;
		
		List<RiskHazardResult> riskHazardResultList = risk01Mapper.selectRiskHazardList(RiskHazardListQuery.from(param));
		
		if(riskHazardResultList.size() > 0) {
			response = RiskHazardListResponse.builder()
											.riskHazardResultList(riskHazardResultList)
											.build();
		}
		
		return response;
	}
	
	public void updateRiskHazard(RiskHazardParam param) {
		for(RiskHazardModel model : param.riskHazardModelList()) {
			
			risk01Mapper.mergeRiskHazard(RiskHazardCommand.from(model));
		}
	}
	
	public void deleteRiskHazard(RiskHazardParam param) {
		for(RiskHazardModel model : param.riskHazardModelList()) {
			
			risk01Mapper.deleteRiskHazard(RiskHazardCommand.from(model));
		}
	}
}
