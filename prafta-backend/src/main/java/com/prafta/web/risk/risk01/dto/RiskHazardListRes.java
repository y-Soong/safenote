package com.prafta.web.risk.risk01.dto;

import java.util.List;

import com.prafta.web.risk.risk01.vo.RiskHazard;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskHazardListRes{
	List<RiskHazard> riskHazardList;
}
