package com.prafta.web.attd.attd01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftTeamInfo{
	String cmpnyCd;
	String siteCd;
	String shiftCd;
	String teamIdx;
	String teamNm;
}
