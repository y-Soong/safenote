package com.prafta.web.risk.risk03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskTypeListReq{
	String processCd;
	String siteCd;
	String riskTypeNm;
	String useYn;
}
