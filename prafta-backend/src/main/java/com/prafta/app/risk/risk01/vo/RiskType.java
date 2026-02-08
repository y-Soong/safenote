package com.prafta.app.risk.risk01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskType {
	String ProcessCd;
	String riskTypeCd;
	String riskTypeNm;
}
