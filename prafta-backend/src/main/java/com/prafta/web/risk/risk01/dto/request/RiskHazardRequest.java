package com.prafta.web.risk.risk01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskHazardRequest{
	private String cmpnyCd;
	private String riskTypeCd;
	private String hazardCd;
	private String hazardNm;
	private String siteCd;
	private String hazardDesc;
}
