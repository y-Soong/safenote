package com.prafta.web.baim.baim04.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesReq;
import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesRes;
import com.prafta.web.baim.baim04.dto.LinkPoliciesReq;

public interface Baim04Service {
	DailyUserLinkPoliciesRes selectDailyUserLinkPolicyList(DailyUserLinkPoliciesReq dto, Map<String, Object> tokenInfo);
	
	void saveDailyUserLinkPolicy(List<LinkPoliciesReq> dtoList, Map<String, Object> tokenInfo);
	
	void deleteDailyUserLinkPolicy(List<LinkPoliciesReq> dtoList, Map<String, Object> tokenInfo);
}
