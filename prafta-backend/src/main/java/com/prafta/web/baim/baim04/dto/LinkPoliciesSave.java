package com.prafta.web.baim.baim04.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LinkPoliciesSave{
	String chk;
	String cmpnyCd;
	String siteCd;
	String fixedYn;
	String useYn;
	String dayLimitCnt;
	String serviceUrl;
}
