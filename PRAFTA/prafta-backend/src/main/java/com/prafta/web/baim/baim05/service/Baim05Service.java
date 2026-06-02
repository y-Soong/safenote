package com.prafta.web.baim.baim05.service;

import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim05.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.dto.response.InsertDailyQrUserResponse;

public interface Baim05Service {
	DailyUserLinkPoliciesResponse selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam param);

	DailyUserSlotListResponse selectDailyUserSlotList(DailyUserSlotListParam param);

	void saveDailyUserLinkPolicy(LinkPoliciesParam param);

	InsertDailyQrUserResponse insertDailyQrUser(InsertDailyQrUserParam param);
}
