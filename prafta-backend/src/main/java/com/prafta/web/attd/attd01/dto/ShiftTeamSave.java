package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftTeamSave{
	String shiftCd;
	String siteCd;
	String teamIdx;
	String teamNm;
}
