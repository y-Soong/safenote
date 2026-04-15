package com.prafta.web.baim.baim01.service;

import com.prafta.web.baim.baim01.application.param.SiteInfoListParam;
import com.prafta.web.baim.baim01.application.param.SiteInfoParam;
import com.prafta.web.baim.baim01.dto.response.SiteInfoListResponse;

public interface Baim01Service {
	SiteInfoListResponse selectSiteInfoList(SiteInfoListParam param);
	
	void saveSiteInfo(SiteInfoParam param);
}
