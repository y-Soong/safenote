package com.prafta.web.baim.baim06.service;

import com.prafta.web.baim.baim06.application.param.CopySiteNodeParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeInfoParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeListParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeParam;
import com.prafta.web.baim.baim06.dto.response.SiteNodeListResponse;

public interface Baim06Service {
	SiteNodeListResponse selectSiteNodeList(SiteNodeListParam param);
	
	void saveSiteNode(SiteNodeInfoParam param);
	
	void deleteSiteNode(SiteNodeParam param);
	
	void deleteSiteAllNode(SiteNodeParam param);
	
	void copySiteNode(CopySiteNodeParam param);
	
	void saveSiteNodeMainAdmin(SiteNodeAdminParam param);
	
	void saveSiteNodeSubAdmin(SiteNodeAdminParam param);
	
	void deleteSiteNodeAdmin(SiteNodeAdminParam param);
}
