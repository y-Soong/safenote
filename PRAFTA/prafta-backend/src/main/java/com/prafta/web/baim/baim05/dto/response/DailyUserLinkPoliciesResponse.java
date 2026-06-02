package com.prafta.web.baim.baim05.dto.response;

import com.prafta.web.baim.baim05.result.DailyUserLinkPolicyResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyUserLinkPoliciesResponse{
	DailyUserLinkPolicyResult dailyUserLinkPolicy;
}
