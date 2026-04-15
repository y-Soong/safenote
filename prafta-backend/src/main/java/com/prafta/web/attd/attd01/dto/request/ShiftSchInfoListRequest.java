package com.prafta.web.attd.attd01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftSchInfoListRequest{
	private String siteCd;
	private String shiftNo;
	private String shiftCycleDays;
	private String useYn;
}
