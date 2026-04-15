package com.prafta.common.cmm.baseinfo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteNodeListRequest {
	private String cmpnyCd;
	private String siteCd;
	private String nodeCd;
	private String nodeType;
	private String nodeNm;
	private String parentNodeNm;
}
