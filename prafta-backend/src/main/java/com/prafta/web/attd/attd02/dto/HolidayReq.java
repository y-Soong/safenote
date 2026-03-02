package com.prafta.web.attd.attd02.dto;

import lombok.Data;

@Data
public class HolidayReq{
	String siteCd;
	String holidayId;
	String holidayNm;
	String holidayYmd;
	String holidayType;
	boolean repeatYearly;
	String useYn;
}
