package com.prafta.web.baim.baim06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteNodeInfoRequest {
	private String siteCd;
	private String nodeCd;
	private String nodeNm;
	private String nodeType;
	private String parentNodeCd;
	private String selfAttdApprvYn;
}
