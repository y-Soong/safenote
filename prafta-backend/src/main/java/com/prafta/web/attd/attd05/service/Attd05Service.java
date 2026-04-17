package com.prafta.web.attd.attd05.service;

import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;

public interface Attd05Service {
	
	UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param);

//    AttdStdTimeRuleListResponse getAttdStdTimeRuleList(AttdStdTimeRuleListParam param);
//
//    void saveAttdStdTimeRule(AttdStdTimeRuleParam param);
}
