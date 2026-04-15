package com.prafta.web.baim.baim05.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.query.DailyUserSlotListQuery;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.mapper.Baim05Mapper;
import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;
import com.prafta.web.baim.baim05.service.Baim05Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim05ServiceImpl implements Baim05Service{
	private final Baim05Mapper baim05Mapper;
		
	public Baim05ServiceImpl(Baim05Mapper baim05Mapper) {
		this.baim05Mapper = baim05Mapper;
	}
	
	public DailyUserSlotListResponse selectDailyUserSlotList(DailyUserSlotListParam param) {
		
		DailyUserSlotListResponse response = null;
		
		List<DailyUserSlotListResult> dailyUserSlotList = baim05Mapper.selectDailyUserSlotList(DailyUserSlotListQuery.from(param));
		
		if(dailyUserSlotList.size() > 0) {
			response = DailyUserSlotListResponse.builder()
					.dailyUserSlotList(dailyUserSlotList)
					.build();
		}
		
		return response;
	}
}
