package com.prafta.web.risk.risk01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskTypeRequest{
	private String cmpnyCd;
	private String processCd;
	private String riskTypeCd;
	private String riskTypeNm;
	private String siteCd;
	private String useYn;
	private String riskTypeDesc;
}
