package com.prafta.web.baim.baim06.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteNodeSave{
	String siteCd;
	String nodeCd;
	String nodeNm;
	String nodeType;
	String parentNodeCd;
	String selfAttdApprvYn;
}
