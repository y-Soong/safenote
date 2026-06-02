package com.prafta.web.baim.baim04.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.baim.baim04.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim04.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim04.application.model.LinkPoliciesModel;
import com.prafta.web.baim.baim04.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim04.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim04.application.param.UserSlotCountQuery;
import com.prafta.web.baim.baim04.application.query.DailyUserLinkPoliciesQuery;
import com.prafta.web.baim.baim04.dto.request.LinkPoliciesRequest;
import com.prafta.web.baim.baim04.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim04.mapper.Baim04Mapper;
import com.prafta.web.baim.baim04.result.DailyUserLinkPolicyResult;
import com.prafta.web.baim.baim04.service.Baim04Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim04ServiceImpl implements Baim04Service{
	private final Baim04Mapper baim04Mapper;
	
	public Baim04ServiceImpl(Baim04Mapper baim04Mapper) {
		this.baim04Mapper = baim04Mapper;
	}

	public DailyUserLinkPoliciesResponse selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam param) {
		
		DailyUserLinkPoliciesResponse response = null;
		
		List<DailyUserLinkPolicyResult> dailyUserLinkPolicyList = baim04Mapper.selectDailyUserLinkPolicyList(DailyUserLinkPoliciesQuery.from(param));
		
		if(dailyUserLinkPolicyList.size() > 0) {
			response = DailyUserLinkPoliciesResponse.builder()
					.dailyUserLinkPolicyList(dailyUserLinkPolicyList)
					.build();
		}
		
		return response;
	}
	
	@Transactional
	public void saveDailyUserLinkPolicy(LinkPoliciesParam param) {
		for(LinkPoliciesModel model : param.linkPoliciesModelList()) {

			baim04Mapper.saveDailyUserLinkPolicy(LinkPoliciesCommand.from(model));
			
			int dayLimitCnt = Integer.parseInt(String.valueOf(model.dayLimitCnt()));
			int dailyUserSlotCnt = baim04Mapper.selectDailyUserSlotCnt(UserSlotCountQuery.from(model));
			
			if(dayLimitCnt > 0 || dailyUserSlotCnt > 0) {
				int limit = 0;
				
				if(dailyUserSlotCnt > 0) {
					if(dailyUserSlotCnt > dayLimitCnt) {
						limit = dailyUserSlotCnt;
					} else {
						limit = dayLimitCnt;
					}
				} else {
					limit = dayLimitCnt;
				}
				
				for(int i = 0; i < limit; i++) {
					String useYn = "";
					
					if(limit == dailyUserSlotCnt) {
						if(i < dayLimitCnt) {
							useYn = "Y";
						} 
						else {
							useYn = "N";
						}
					}
					
					baim04Mapper.saveDailyUserSlot(DailyUserSlotCommand.from(model, dailyUserSlotCnt, useYn));
				}
			}
		}
	}
	
	public void deleteDailyUserLinkPolicy(LinkPoliciesParam param) {
		for(LinkPoliciesModel model : param.linkPoliciesModelList()) {
			
			baim04Mapper.deleteDailyUserLinkPolicy(DailyUserSlotCommand.from(model));
		}
	}
}
