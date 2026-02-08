package com.prafta.web.risk.risk01.dto;

import lombok.Data;

@Data
public class RiskTypeReq{
	String cmpnyCd;
	String processCd;
	String riskTypeCd;
	String riskTypeNm;
	String siteCd;
	String useYn;
	String riskTypeDesc;
}
