package com.prafta.web.risk.risk01.dto;

import java.util.List;

import com.prafta.web.risk.risk01.vo.RiskType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RiskTypeListRes{
	List<RiskType> riskTypeList;
}
