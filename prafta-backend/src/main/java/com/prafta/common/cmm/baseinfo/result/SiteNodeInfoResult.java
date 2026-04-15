package com.prafta.common.cmm.baseinfo.result;

public record SiteNodeInfoResult(
	String cmpnyCd
	, String siteCd
	, String nodeCd
	, String nodeNm
	, String nodeType
	, String parentNodeCd
	, String parentNodeNm
	, String selfAttdApprvYn
) {
	
}
