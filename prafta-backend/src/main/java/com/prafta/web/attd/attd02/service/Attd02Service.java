package com.prafta.web.attd.attd02.service;

import com.prafta.web.attd.attd02.application.param.HolidayListParam;
import com.prafta.web.attd.attd02.application.param.HolidayParam;
import com.prafta.web.attd.attd02.dto.response.HolidayListResponse;

public interface Attd02Service {
	
	HolidayListResponse selectHoliday(HolidayListParam param);
	
	void updateHolidayInfo(HolidayParam param);
}
