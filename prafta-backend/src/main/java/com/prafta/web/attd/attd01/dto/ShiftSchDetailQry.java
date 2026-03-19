package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchDetailQry{
	String siteCd;
	String shiftCd;
}
