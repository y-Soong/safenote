package com.prafta.web.attd.attd01.service;

import java.util.Map;

import com.prafta.web.attd.attd01.dto.SchInfoListReq;
import com.prafta.web.attd.attd01.dto.SchInfoListRes;

public interface Attd01Service {
	SchInfoListRes selectSchInfoList(SchInfoListReq dto, Map<String, Object> tokenInfo);
	
//	DailyUserSlotListRes selectDailyUserSlotList(DailyUserSlotListReq dto, Map<String, Object> tokenInfo);
}
