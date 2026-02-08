package com.prafta.web.baim.baim04.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesQry;
import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesReq;
import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesRes;
import com.prafta.web.baim.baim04.dto.DailyUserSlotSave;
import com.prafta.web.baim.baim04.dto.LinkPoliciesReq;
import com.prafta.web.baim.baim04.dto.LinkPoliciesSave;
import com.prafta.web.baim.baim04.mapper.Baim04Mapper;
import com.prafta.web.baim.baim04.service.Baim04Service;
import com.prafta.web.baim.baim04.vo.DailyUserLinkPolicy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim04ServiceImpl implements Baim04Service{
	private final Baim04Mapper baim04Mapper;
	
	public Baim04ServiceImpl(Baim04Mapper baim04Mapper) {
		this.baim04Mapper = baim04Mapper;
	}

	public DailyUserLinkPoliciesRes selectDailyUserLinkPolicyList(DailyUserLinkPoliciesReq dto, Map<String, Object> tokenInfo) {
		
		DailyUserLinkPoliciesQry reqDto = DailyUserLinkPoliciesQry.builder()
											.siteCd(dto.getSiteCd())
											.useYn(dto.getUseYn())
											.build();
		
		DailyUserLinkPoliciesRes retDto = null;
		
		List<DailyUserLinkPolicy> dailyUserLinkPolicyList = baim04Mapper.selectDailyUserLinkPolicyList(reqDto, tokenInfo);
		
		if(dailyUserLinkPolicyList.size() > 0) {
			retDto = DailyUserLinkPoliciesRes.builder()
					.dailyUserLinkPolicyList(dailyUserLinkPolicyList)
					.build();
		}
		
		return retDto;
	}
	
	@Transactional
	public void saveDailyUserLinkPolicy(List<LinkPoliciesReq> dtoList, Map<String, Object> tokenInfo) {
		for(LinkPoliciesReq dto : dtoList) {
			LinkPoliciesSave reqDto = LinkPoliciesSave.builder()
										.cmpnyCd(dto.getCmpnyCd())
										.siteCd(dto.getSiteCd())
										.useYn(dto.getUseYn())
										.dayLimitCnt(dto.getDayLimitCnt())
										.build();
			
			baim04Mapper.saveDailyUserLinkPolicy(reqDto, tokenInfo);
			
			int dayLimitCnt = Integer.parseInt(String.valueOf(reqDto.getDayLimitCnt()));
			int dailyUserSlotCnt = baim04Mapper.selectDailyUserSlotCnt(reqDto, tokenInfo);
			
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
					DailyUserSlotSave dailyUserSlotSave = DailyUserSlotSave.builder()
															.cmpnyCd((String)tokenInfo.get("gv_cmpnyCd"))
															.siteCd(reqDto.getSiteCd())
															.slotNo(i)
															.slotType("01")				// 01:일반사용자, 02:QR사용자 
															.slotStatus("01")			// 01:비점유중, 02:점유중
															.build();
					
					if(limit == dailyUserSlotCnt) {
						if(i < dayLimitCnt) {
							dailyUserSlotSave = dailyUserSlotSave.toBuilder().useYn("Y").build();
						} 
						else {
							dailyUserSlotSave = dailyUserSlotSave.toBuilder().useYn("N").build();
						}
					}
					
					baim04Mapper.saveDailyUserSlot(dailyUserSlotSave, tokenInfo);
				}
			}
		}
	}
	
	public void deleteDailyUserLinkPolicy(List<LinkPoliciesReq> dtoList, Map<String, Object> tokenInfo) {
		for(LinkPoliciesReq dto : dtoList) {
			LinkPoliciesSave reqDto = LinkPoliciesSave.builder()
										.cmpnyCd(dto.getCmpnyCd())
										.siteCd(dto.getSiteCd())
										.useYn(dto.getUseYn())
										.dayLimitCnt(dto.getDayLimitCnt())
										.build();
			
			baim04Mapper.deleteDailyUserLinkPolicy(reqDto, tokenInfo);
		}
	}
}
