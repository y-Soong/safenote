package com.prafta.web.risk.risk01.dto.response;

import java.util.List;

import com.prafta.web.risk.risk01.result.RiskTypeResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskTypeListResponse{
	private List<RiskTypeResult> riskTypeResultList;
}
