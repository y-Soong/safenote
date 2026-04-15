package com.prafta.web.baim.baim05.service;

import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;

public interface Baim05Service {
	DailyUserSlotListResponse selectDailyUserSlotList(DailyUserSlotListParam param);
}
