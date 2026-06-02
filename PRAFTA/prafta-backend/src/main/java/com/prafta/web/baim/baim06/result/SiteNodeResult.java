package com.prafta.web.baim.baim06.result;

public record SiteNodeResult(
	String cmpnyCd
	, String siteCd
	, String nodeCd
	, String nodeNm
	, String nodeType
	, String parentNodeCd
	, String selfAttdApprvYn
	, String mainAdminCd
	, String mainAdminNm
	, String subAdminCd
	, String subAdminNm
	, String managerCnt
	, String workerCnt
){
	
}
