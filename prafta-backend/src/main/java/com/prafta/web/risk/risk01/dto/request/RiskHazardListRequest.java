package com.prafta.web.risk.risk01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskHazardListRequest{
	private String riskTypeCd;
	private String hazardNm;
	private String hazardDesc;
}
