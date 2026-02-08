package com.prafta.web.risk.risk01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskHazard{
	String cmpnyCd;
	String riskTypeCd;
	String hazardCd;
	String hazardNm;
	String siteCd;
	String hazardDesc;
}
