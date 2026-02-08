package com.prafta.web.risk.risk01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskType{
	String cmpnyCd;
	String riskTypeCd;
	String riskTypeNm;
	String siteCd;
	String processCd;
	String useYn;
	String riskTypeDesc;
}
