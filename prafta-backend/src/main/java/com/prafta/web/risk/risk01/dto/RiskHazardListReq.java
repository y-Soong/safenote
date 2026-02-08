package com.prafta.web.risk.risk01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskHazardListReq{
	String riskTypeCd;
	String hazardNm;
	String hazardDesc;
}
