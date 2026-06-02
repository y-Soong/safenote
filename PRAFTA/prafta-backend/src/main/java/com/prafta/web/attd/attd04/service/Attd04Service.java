package com.prafta.web.attd.attd04.service;

import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleListParam;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleParam;
import com.prafta.web.attd.attd04.dto.response.AttdStdTimeRuleListResponse;

public interface Attd04Service {

    AttdStdTimeRuleListResponse getAttdStdTimeRuleList(AttdStdTimeRuleListParam param);

    void saveAttdStdTimeRule(AttdStdTimeRuleParam param);
}
