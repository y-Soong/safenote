package com.prafta.app.risk.risk01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskHazard {
	String riskTypeCd;
	String hazardCd;
	String hazardNm;
}
