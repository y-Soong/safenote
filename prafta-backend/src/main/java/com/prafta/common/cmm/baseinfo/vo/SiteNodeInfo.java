package com.prafta.common.cmm.baseinfo.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteNodeInfo {
	String cmpnyCd;
	String siteCd;
	String nodeCd;
	String nodeNm;
	String nodeType;
	String parentNodeCd;
	String parentNodeNm;
	String selfAttdApprvYn;
}
