package com.prafta.web.baim.baim04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkPoliciesRequest{
	private String chk;
	private String cmpnyCd;
	private String siteCd;
	private String useYn;
	private String dayLimitCnt;
	private String serviceUrl;
}
