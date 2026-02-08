package com.prafta.common.cmm.baseinfo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteNodeListQry {
	String cmpnyCd;
	String siteCd;
	String nodeCd;
	String nodeType;
	String nodeNm;
	String parentNodeNm;
}
