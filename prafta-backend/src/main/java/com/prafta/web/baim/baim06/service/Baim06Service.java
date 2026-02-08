package com.prafta.web.baim.baim06.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim06.dto.CopySiteNodeReq;
import com.prafta.web.baim.baim06.dto.SiteNodeListReq;
import com.prafta.web.baim.baim06.dto.SiteNodeListRes;
import com.prafta.web.baim.baim06.dto.SiteNodeReq;

public interface Baim06Service {
	SiteNodeListRes selectSiteNodeList(SiteNodeListReq dto, Map<String, Object> tokenInfo);
	
	void saveSiteNode(List<SiteNodeReq> dtoList, Map<String, Object> tokenInfo);
	
	void deleteSiteNode(SiteNodeReq dto, Map<String, Object> tokenInfo);
	
	void deleteSiteAllNode(SiteNodeReq dto, Map<String, Object> tokenInfo);
	
	void copySiteNode(CopySiteNodeReq dto, Map<String, Object> tokenInfo);
}
