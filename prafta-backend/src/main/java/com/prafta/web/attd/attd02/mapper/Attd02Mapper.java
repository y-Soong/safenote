package com.prafta.web.attd.attd02.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd02.application.command.HolidayCommand;
import com.prafta.web.attd.attd02.application.query.HolidayListQuery;
import com.prafta.web.attd.attd02.result.HolidayResult;

@Mapper
public interface Attd02Mapper {
	
	List<HolidayResult> selectHoliday(HolidayListQuery query);
	
	String selectHolidayRuleId(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	String selectHolidayId(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	void updateHolidayRule(HolidayCommand command);
	
	void updateHoliday(HolidayCommand command);
}
