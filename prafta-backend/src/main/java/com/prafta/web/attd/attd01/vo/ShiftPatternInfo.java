package com.prafta.web.attd.attd01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftPatternInfo{
	String cmpnyCd;
	String siteCd;
	String shiftCd;
	String ptrnIdx;
	String schCd;
}
