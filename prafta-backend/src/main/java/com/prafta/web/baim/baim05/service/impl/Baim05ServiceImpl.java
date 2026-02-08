package com.prafta.web.baim.baim05.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.baim.baim05.dto.DailyUserSlotListQry;
import com.prafta.web.baim.baim05.dto.DailyUserSlotListReq;
import com.prafta.web.baim.baim05.dto.DailyUserSlotListRes;
import com.prafta.web.baim.baim05.mapper.Baim05Mapper;
import com.prafta.web.baim.baim05.service.Baim05Service;
import com.prafta.web.baim.baim05.vo.DailyUserSlotList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim05ServiceImpl implements Baim05Service{
	private final Baim05Mapper baim05Mapper;
		
	public Baim05ServiceImpl(Baim05Mapper baim05Mapper) {
		this.baim05Mapper = baim05Mapper;
	}
	
	public DailyUserSlotListRes selectDailyUserSlotList(DailyUserSlotListReq dto, Map<String, Object> tokenInfo) {
		
		DailyUserSlotListQry reqDto = DailyUserSlotListQry.builder()
										.siteCd(dto.getSiteCd())
										.slotType(dto.getSlotType())
										.slotStatus(dto.getSlotStatus())
										.useYn(dto.getUseYn())
										.currUserId(dto.getCurrUserId())
										.build();
		
		DailyUserSlotListRes retDto = null;
		
		List<DailyUserSlotList> dailyUserSlotList = baim05Mapper.selectDailyUserSlotList(reqDto, tokenInfo);
		
		if(dailyUserSlotList.size() > 0) {
			retDto = DailyUserSlotListRes.builder()
					.dailyUserSlotList(dailyUserSlotList)
					.build();
		}
		
		return retDto;
	}
}
