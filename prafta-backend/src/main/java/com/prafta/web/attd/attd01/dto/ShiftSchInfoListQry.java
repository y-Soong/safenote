package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchInfoListQry{
	String siteCd;
	String shiftNo;
	String shiftCycleDays;
	String useYn;
}
