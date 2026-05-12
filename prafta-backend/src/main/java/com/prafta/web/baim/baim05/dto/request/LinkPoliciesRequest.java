package com.prafta.web.baim.baim05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkPoliciesRequest{
	private String siteCd;
	private String useYn;
	private String dayLimitCnt;
}
