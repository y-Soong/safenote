package com.prafta.web.attd.attd02.service;

import java.util.Map;

import com.prafta.web.attd.attd02.dto.HolidayListReq;
import com.prafta.web.attd.attd02.dto.HolidayListRes;
import com.prafta.web.attd.attd02.dto.HolidayReq;

public interface Attd02Service {
	
	HolidayListRes selectHoliday(HolidayListReq dto, Map<String, Object> tokenInfo);
	
	void updateHolidayInfo(HolidayReq dto, Map<String, Object> tokenInfo);
	
//	SchInfoListRes selectSchInfoList(SchInfoListReq dto, Map<String, Object> tokenInfo);
//	
//	void updateSchInfo(SchInfoReq dto, Map<String, Object> tokenInfo);
//	
//	SchInfoHistRes selectSchHistList(SchInfoHistReq dto, Map<String, Object> tokenInfo);
}
