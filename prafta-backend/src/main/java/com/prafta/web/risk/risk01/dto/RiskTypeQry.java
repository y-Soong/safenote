package com.prafta.web.risk.risk01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskTypeQry{
	String cmpnyCd;
	String processCd;
	String riskTypeCd;
	String riskTypeNm;
	String siteCd;
	String useYn;
	String riskTypeDesc;
}
