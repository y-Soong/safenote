package com.prafta.web.baim.baim04.service;

import com.prafta.web.baim.baim04.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim04.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim04.dto.response.DailyUserLinkPoliciesResponse;

public interface Baim04Service {
	DailyUserLinkPoliciesResponse selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam param);
	
	void saveDailyUserLinkPolicy(LinkPoliciesParam param);
	
	void deleteDailyUserLinkPolicy(LinkPoliciesParam param);
}
