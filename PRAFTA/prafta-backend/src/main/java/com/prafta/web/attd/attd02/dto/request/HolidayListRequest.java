package com.prafta.web.attd.attd02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HolidayListRequest{
	private String year;
	private String month;
}
