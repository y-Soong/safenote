package com.prafta.web.risk.risk01.dto.response;

import java.util.List;

import com.prafta.web.risk.risk01.result.RiskHazardResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskHazardListResponse{
	private List<RiskHazardResult> riskHazardResultList;
}
