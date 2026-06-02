package com.prafta.web.baim.baim02.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.baim.baim02.application.command.CompCmmCodeDCommand;
import com.prafta.web.baim.baim02.application.model.CompCmmCodeDModel;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeDListParam;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeDParam;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeMListParam;
import com.prafta.web.baim.baim02.application.query.CompCmmCodeDListQuery;
import com.prafta.web.baim.baim02.application.query.CompCmmCodeMListQuery;
import com.prafta.web.baim.baim02.dto.response.CompCmmCodeDListResponse;
import com.prafta.web.baim.baim02.dto.response.CompCmmCodeMListResponse;
import com.prafta.web.baim.baim02.mapper.Baim02Mapper;
import com.prafta.web.baim.baim02.result.CompCmmCodeDResult;
import com.prafta.web.baim.baim02.result.CompCmmCodeMResult;
import com.prafta.web.baim.baim02.service.Baim02Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim02ServiceImpl implements Baim02Service{
	private final Baim02Mapper baim02Mapper; 
	
	public Baim02ServiceImpl(Baim02Mapper baim02Mapper) {
		this.baim02Mapper = baim02Mapper;
	}
		
	public CompCmmCodeMListResponse selectCompCmmCodeMList(CompCmmCodeMListParam param) {
		
		CompCmmCodeMListResponse response = null;
				
		List<CompCmmCodeMResult> compCmmCodeMList = baim02Mapper.selectCompCmmCodeMList(CompCmmCodeMListQuery.from(param));
		
		if(compCmmCodeMList != null && compCmmCodeMList.size() > 0) {
			response = CompCmmCodeMListResponse.builder()
										.compCmmCodeMList(compCmmCodeMList)
										.build();
		}
		
		return response;
	}

	public CompCmmCodeDListResponse selectCompCmmCodeDList(CompCmmCodeDListParam param) {
		
		CompCmmCodeDListResponse response = null;
		
		List<CompCmmCodeDResult> compCmmCodeDList = baim02Mapper.selectCompCmmCodeDList(CompCmmCodeDListQuery.from(param));
		
		if(compCmmCodeDList != null && compCmmCodeDList.size() > 0) {
			response = CompCmmCodeDListResponse.builder()
										.compCmmCodeDList(compCmmCodeDList)
										.build();
		}
		
		return response;
	}
	
	public void updateCmmCodeDetailInfo(CompCmmCodeDParam param) {
		for(CompCmmCodeDModel model : param.compCmmCodeDModelList()) {			
			baim02Mapper.mergeCmmCodeDetailInfo(CompCmmCodeDCommand.from(model));
		}
	}
	
	public void deleteCmmCodeDetailInfo(CompCmmCodeDParam param) {
		
		for(CompCmmCodeDModel model : param.compCmmCodeDModelList()) {
			baim02Mapper.deleteCmmCodeDetailInfo(CompCmmCodeDCommand.from(model));
		}
	}
	
}
