package com.prafta.web.attd.attd02.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HolidayListQry{
	String year;
	String month;
}
