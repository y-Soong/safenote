package com.prafta.web.risk.risk01.dto;

import lombok.Data;

@Data
public class RiskHazardReq{
	String cmpnyCd;
	String riskTypeCd;
	String hazardCd;
	String hazardNm;
	String siteCd;
	String hazardDesc;
}
