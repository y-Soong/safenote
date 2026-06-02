package com.prafta.web.risk.risk01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskTypeListRequest{
	private String processCd;
	private String siteCd;
	private String riskTypeNm;
	private String useYn;
}
