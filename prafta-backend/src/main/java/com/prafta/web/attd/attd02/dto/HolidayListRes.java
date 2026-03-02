package com.prafta.web.attd.attd02.dto;

import java.util.List;

import com.prafta.web.attd.attd02.vo.Holiday;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HolidayListRes{
	List<Holiday> holidayList;
}
