package com.prafta.web.risk.risk03.dto.response;

import java.util.List;

import com.prafta.web.risk.risk03.result.RiskTypeResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskTypeListResponse{
	List<RiskTypeResult> riskTypeResultList;
}
