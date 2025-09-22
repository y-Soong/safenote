package com.prafta.web.baim.baim01.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim01.dto.Baim01;
import com.prafta.web.baim.baim01.dto.Baim01ReqDto;

public interface Baim01Service {
	List<Map<String, Object>> selectSiteInfoList(Baim01ReqDto dto);
	
	void updateSiteInfo(List<Baim01> dtoList, Map<String, Object> tokenInfo);
}
