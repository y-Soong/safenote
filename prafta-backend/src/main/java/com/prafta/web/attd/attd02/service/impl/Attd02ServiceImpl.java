package com.prafta.web.attd.attd02.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd02.application.command.HolidayCommand;
import com.prafta.web.attd.attd02.application.param.HolidayListParam;
import com.prafta.web.attd.attd02.application.param.HolidayParam;
import com.prafta.web.attd.attd02.application.query.HolidayListQuery;
import com.prafta.web.attd.attd02.dto.response.HolidayListResponse;
import com.prafta.web.attd.attd02.mapper.Attd02Mapper;
import com.prafta.web.attd.attd02.result.HolidayResult;
import com.prafta.web.attd.attd02.service.Attd02Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd02ServiceImpl implements Attd02Service{
	private final Attd02Mapper attd02Mapper;
		
	public Attd02ServiceImpl(Attd02Mapper attd02Mapper) {
		this.attd02Mapper = attd02Mapper;
	}
	
	public HolidayListResponse selectHoliday(HolidayListParam param) {

		HolidayListResponse response = null;
		
		List<HolidayResult> holidayResultList = attd02Mapper.selectHoliday(HolidayListQuery.from(param));
		
		if(holidayResultList != null && holidayResultList.size() > 0) {
			response = HolidayListResponse.builder()
											.holidayResultList(holidayResultList)
											.build();
		}
		
		return response;
	}
	
	public void updateHolidayInfo(HolidayParam param) {
		
		String holidayId = null;
		String holidayType = null;
		
		if(!param.holidayId().isEmpty()) {
			holidayId = param.holidayId();
			holidayType = param.holidayType();
		} else {
			if(param.repeatYearly()) {
				holidayId = attd02Mapper.selectHolidayRuleId(param.gvCmpnyCd());
				holidayType = "03";			/* 반복 */
			} else {
				holidayId = attd02Mapper.selectHolidayId(param.gvCmpnyCd());
				holidayType = "02";			/* 반복 */
			}
		}
		
		if(param.repeatYearly()) {
			attd02Mapper.updateHolidayRule(HolidayCommand.from(param, holidayId, holidayType));
		} else {
			attd02Mapper.updateHoliday(HolidayCommand.from(param, holidayId, holidayType));
		}
	}
}
