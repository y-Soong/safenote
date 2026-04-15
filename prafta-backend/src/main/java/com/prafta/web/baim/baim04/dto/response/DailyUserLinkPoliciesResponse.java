package com.prafta.web.baim.baim04.dto.response;

import java.util.List;

import com.prafta.web.baim.baim04.result.DailyUserLinkPolicyResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyUserLinkPoliciesResponse{
	List<DailyUserLinkPolicyResult> dailyUserLinkPolicyList;
}
