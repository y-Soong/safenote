package com.prafta.web.attd.attd01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd01.dto.SchInfoListQry;
import com.prafta.web.attd.attd01.dto.SchInfoListReq;
import com.prafta.web.attd.attd01.dto.SchInfoListRes;
import com.prafta.web.attd.attd01.mapper.Attd01Mapper;
import com.prafta.web.attd.attd01.service.Attd01Service;
import com.prafta.web.attd.attd01.vo.SchInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd01ServiceImpl implements Attd01Service{
	private final Attd01Mapper attd01Mapper;
		
	public Attd01ServiceImpl(Attd01Mapper attd01Mapper) {
		this.attd01Mapper = attd01Mapper;
	}
	
	public SchInfoListRes selectSchInfoList(SchInfoListReq dto, Map<String, Object> tokenInfo) {
		SchInfoListQry reqDto = SchInfoListQry.builder()
												.siteCd(dto.getSiteCd())
												.schNo(dto.getSchNo())
												.schType(dto.getSchType())
												.useYn(dto.getUseYn())
												.build();
		
		SchInfoListRes resDto = null;
		
		List<SchInfo> schInfoList = attd01Mapper.selectSchInfoList(reqDto, tokenInfo);
		
		if(schInfoList != null && !schInfoList.isEmpty()) {
			resDto = SchInfoListRes.builder()
									.schInfoList(schInfoList)
									.build();
		}
		
		return resDto;
	
	}
	
//	public DailyUserSlotListRes selectDailyUserSlotList(DailyUserSlotListReq dto, Map<String, Object> tokenInfo) {
//		
//		DailyUserSlotListQry reqDto = DailyUserSlotListQry.builder()
//										.siteCd(dto.getSiteCd())
//										.slotType(dto.getSlotType())
//										.slotStatus(dto.getSlotStatus())
//										.useYn(dto.getUseYn())
//										.currUserId(dto.getCurrUserId())
//										.build();
//		
//		DailyUserSlotListRes retDto = null;
//		
//		List<DailyUserSlotList> dailyUserSlotList = baim05Mapper.selectDailyUserSlotList(reqDto, tokenInfo);
//		
//		if(dailyUserSlotList.size() > 0) {
//			retDto = DailyUserSlotListRes.builder()
//					.dailyUserSlotList(dailyUserSlotList)
//					.build();
//		}
//		
//		return retDto;
//	}
}
