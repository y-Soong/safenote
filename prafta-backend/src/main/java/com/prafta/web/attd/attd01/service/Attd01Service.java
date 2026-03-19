package com.prafta.web.attd.attd01.service;

import java.util.Map;

import com.prafta.web.attd.attd01.dto.SchInfoHistReq;
import com.prafta.web.attd.attd01.dto.SchInfoHistRes;
import com.prafta.web.attd.attd01.dto.SchInfoListReq;
import com.prafta.web.attd.attd01.dto.SchInfoListRes;
import com.prafta.web.attd.attd01.dto.SchInfoReq;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailReq;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailRes;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListReq;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListRes;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoReq;

public interface Attd01Service {
	SchInfoListRes selectSchInfoList(SchInfoListReq dto, Map<String, Object> tokenInfo);
	
	void updateSchInfo(SchInfoReq dto, Map<String, Object> tokenInfo);
	
	SchInfoHistRes selectSchHistList(SchInfoHistReq dto, Map<String, Object> tokenInfo);
	
	void updateShiftSchInfo(ShiftSchInfoReq dto, Map<String, Object> tokenInfo);
	
	ShiftSchInfoListRes selectShiftSchInfoList(ShiftSchInfoListReq dto, Map<String, Object> tokenInfo);
	
	ShiftSchDetailRes selectShiftSchDetail(ShiftSchDetailReq dto, Map<String, Object> tokenInfo);
	
}
