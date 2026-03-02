package com.prafta.common.schedule.holiday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidayItem {
    public String dateName;   // ¸íÄª
    public Integer locdate;   // yyyymmdd
    public String isHoliday;  // Y/N
}