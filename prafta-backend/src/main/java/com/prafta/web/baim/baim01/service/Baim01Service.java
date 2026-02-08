package com.prafta.web.baim.baim01.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim01.dto.SiteInfoListReq;
import com.prafta.web.baim.baim01.dto.SiteInfoListRes;
import com.prafta.web.baim.baim01.dto.SiteInfoReq;

public interface Baim01Service {
	SiteInfoListRes selectSiteInfoList(SiteInfoListReq dto, Map<String, Object> tokenInfo);
	
	void saveSiteInfo(List<SiteInfoReq> dtoList, Map<String, Object> tokenInfo);
}
