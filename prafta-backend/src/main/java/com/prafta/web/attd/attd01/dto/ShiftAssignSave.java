package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftAssignSave{
	String shiftCd;
	String siteCd;
	String dayNo;
	String teamIdx;
	String assignYn;
	String schCd;
}
