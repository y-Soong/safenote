package com.prafta.web.risk.risk01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskTypeListQry{
	String processCd;
	String siteCd;
	String riskTypeNm;
	String useYn;
}
