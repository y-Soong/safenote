package com.prafta.web.risk.risk03.dto;

import java.util.List;

import com.prafta.web.risk.risk03.vo.RiskType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskTypeListRes{
	List<RiskType> riskTypeList;
}
