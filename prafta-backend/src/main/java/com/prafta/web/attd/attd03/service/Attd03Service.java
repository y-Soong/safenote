package com.prafta.web.attd.attd03.service;

import java.util.Map;

import com.prafta.web.attd.attd03.dto.LeaveTypeListReq;
import com.prafta.web.attd.attd03.dto.LeaveTypeListRes;
import com.prafta.web.attd.attd03.dto.LeaveTypeReq;

public interface Attd03Service {
	
	void updateLeaveType(LeaveTypeReq dto, Map<String, Object> tokenInfo);
	
	LeaveTypeListRes getLeaves(LeaveTypeListReq dto, Map<String, Object> tokenInfo);
	
//	HolidayListRes selectHoliday(HolidayListReq dto, Map<String, Object> tokenInfo);
//	
//	void updateHolidayInfo(HolidayReq dto, Map<String, Object> tokenInfo);
//	
//	SchInfoListRes selectSchInfoList(SchInfoListReq dto, Map<String, Object> tokenInfo);
//	
//	void updateSchInfo(SchInfoReq dto, Map<String, Object> tokenInfo);
//	
//	SchInfoHistRes selectSchHistList(SchInfoHistReq dto, Map<String, Object> tokenInfo);
}
