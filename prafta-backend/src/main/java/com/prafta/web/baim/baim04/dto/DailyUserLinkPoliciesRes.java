package com.prafta.web.baim.baim04.dto;

import java.util.List;

import com.prafta.web.baim.baim04.vo.DailyUserLinkPolicy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyUserLinkPoliciesRes{
	List<DailyUserLinkPolicy> dailyUserLinkPolicyList;
}
