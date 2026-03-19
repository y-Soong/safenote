package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchInfoListReq{
	String siteCd;
	String shiftNo;
	String shiftCycleDays;
	String useYn;
}
