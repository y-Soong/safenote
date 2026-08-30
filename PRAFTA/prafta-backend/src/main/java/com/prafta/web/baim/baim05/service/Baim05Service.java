package com.prafta.web.baim.baim05.service;

import com.prafta.web.baim.baim05.application.param.CheckDailyUserPhoneParam;
import com.prafta.web.baim.baim05.application.param.ClearDailyUserSlotsParam;
import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.SetSlotFixedParam;
import com.prafta.web.baim.baim05.application.param.SetSlotNodeParam;
import com.prafta.web.baim.baim05.application.param.SetSlotSchParam;
import com.prafta.web.baim.baim05.application.param.SetSlotTypeParam;
import com.prafta.web.baim.baim05.application.param.SlotHisParam;
import com.prafta.web.baim.baim05.dto.response.CheckDailyUserPhoneResponse;
import com.prafta.web.baim.baim05.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.dto.response.InsertDailyQrUserResponse;
import com.prafta.web.baim.baim05.dto.response.SlotHisListResponse;

public interface Baim05Service {
	DailyUserLinkPoliciesResponse selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam param);

	DailyUserSlotListResponse selectDailyUserSlotList(DailyUserSlotListParam param);

	void saveDailyUserLinkPolicy(LinkPoliciesParam param);

	InsertDailyQrUserResponse insertDailyQrUser(InsertDailyQrUserParam param);

	void clearDailyUserSlots(ClearDailyUserSlotsParam param);

	void setDailyUserSlotFixed(SetSlotFixedParam param);

	void setDailyUserSlotType(SetSlotTypeParam param);

	void setDailyUserSlotNode(SetSlotNodeParam param);

	void setDailyUserSlotSch(SetSlotSchParam param);

	CheckDailyUserPhoneResponse checkDailyUserPhone(CheckDailyUserPhoneParam param);

	SlotHisListResponse selectDailyUserSlotHisList(SlotHisParam param);
}
