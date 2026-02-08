package com.prafta.web.risk.risk03.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskType{
	String processCd;
	String riskTypeCd;
	String riskTypeNm;
}
