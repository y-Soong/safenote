package com.prafta.web.attd.attd01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftTypeInfo{
	String cmpnyCd;
	String siteCd;
	String shiftCd;
	String shiftNo;
	String shiftPtrnCnt;
	String shiftTeamCnt;
	String shiftCycleDays;
	String useYn;
}
