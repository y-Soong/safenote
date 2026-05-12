package com.prafta.web.attd.attd05.service;

import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeDeleParam;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeParam;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.dto.response.LeaveTypeResponse;
import com.prafta.web.attd.attd05.dto.response.SchTypeListResponse;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;

public interface Attd05Service {
	
	UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param);
	
	SchTypeListResponse getSchTypeList(SchTypeListParam param);
	
	LeaveTypeResponse getLeaveTypeList(LeaveTypeListParam param);
	
	void saveUserWorkPlans(SchTypeParam param);
	
	void deleteUserWorkPlans(SchTypeDeleParam param);
}
