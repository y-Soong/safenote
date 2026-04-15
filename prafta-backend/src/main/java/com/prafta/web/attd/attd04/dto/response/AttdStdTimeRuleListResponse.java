package com.prafta.web.attd.attd04.dto.response;

import java.util.List;

import com.prafta.web.attd.attd04.result.AttdStdTimeRuleHistResult;
import com.prafta.web.attd.attd04.result.AttdStdTimeRuleResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdStdTimeRuleListResponse {

    List<AttdStdTimeRuleResult> attdStdTimeRuleResultList;
    List<AttdStdTimeRuleHistResult> attdStdTimeRuleHistResultList;
}
