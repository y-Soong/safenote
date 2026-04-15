package com.prafta.web.baim.baim06.application.model;

public record SiteNodeModel(
	String siteCd
	, String nodeCd
	, String nodeNm
	, String nodeType
	, String parentNodeCd
	, String selfAttdApprvYn
	, String gvCmpnyCd
	, String gvUserCd
) {
	
}
