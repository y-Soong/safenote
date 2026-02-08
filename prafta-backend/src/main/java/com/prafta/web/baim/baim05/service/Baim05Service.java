package com.prafta.web.baim.baim05.service;

import java.util.Map;

import com.prafta.web.baim.baim05.dto.DailyUserSlotListReq;
import com.prafta.web.baim.baim05.dto.DailyUserSlotListRes;

public interface Baim05Service {
	DailyUserSlotListRes selectDailyUserSlotList(DailyUserSlotListReq dto, Map<String, Object> tokenInfo);
}
