package com.prafta.web.risk.risk01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskHazardQry{
	String cmpnyCd;
	String riskTypeCd;
	String hazardCd;
	String hazardNm;
	String siteCd;
	String hazardDesc;
}
