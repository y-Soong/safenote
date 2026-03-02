package com.prafta.web.attd.attd02.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Holiday{
	String cmpnyCd;
	String holidayId;
	String holidayNm;
	String holidayYmd;
	String holidayType;
	String useYn;
	String insertNo;
	String insertNm;
	String insertDate;	
}
