package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftTypeSave{
	String siteCd;
	String shiftCd;
	String shiftNo;
	String shiftPtrnCnt;
	String shiftTeamCnt;
	String shiftCycleDays;
	String useYn;
}
