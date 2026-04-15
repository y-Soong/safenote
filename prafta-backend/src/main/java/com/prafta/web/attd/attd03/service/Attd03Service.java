package com.prafta.web.attd.attd03.service;

import com.prafta.web.attd.attd03.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;
import com.prafta.web.attd.attd03.dto.response.LeaveTypeListResponse;

public interface Attd03Service {
	
	void updateLeaveType(LeaveTypeParam param);
	
	LeaveTypeListResponse getLeaves(LeaveTypeListParam param);
}
