package com.prafta.web.attd.attd01.service;

import com.prafta.web.attd.attd01.application.param.SchInfoHistParam;
import com.prafta.web.attd.attd01.application.param.SchInfoListParam;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchDetailParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoListParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam;
import com.prafta.web.attd.attd01.dto.response.SchInfoHistResponse;
import com.prafta.web.attd.attd01.dto.response.SchInfoListResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchDetailResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchInfoListResponse;

public interface Attd01Service {
	SchInfoListResponse selectSchInfoList(SchInfoListParam param);
	
	void updateSchInfo(SchInfoParam param);
	
	SchInfoHistResponse selectSchHistList(SchInfoHistParam param);
	
	void updateShiftSchInfo(ShiftSchInfoParam param);
	
	ShiftSchInfoListResponse selectShiftSchInfoList(ShiftSchInfoListParam param);
	
	ShiftSchDetailResponse selectShiftSchDetail(ShiftSchDetailParam param);
	
}
