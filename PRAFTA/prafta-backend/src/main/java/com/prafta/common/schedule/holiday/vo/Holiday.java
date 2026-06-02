package com.prafta.common.schedule.holiday.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Holiday {
	private String cmpnyCd;
	private String holidayId;
	private String holidayDt;   // YYYY-MM-DD
    private String holidayNm;
    private String isHoliday;   // Y/N
    private int year;
}
