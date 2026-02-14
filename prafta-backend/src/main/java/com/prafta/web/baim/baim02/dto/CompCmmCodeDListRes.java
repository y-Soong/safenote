package com.prafta.web.baim.baim02.dto;

import java.util.List;

import com.prafta.web.baim.baim02.vo.CompCmmCodeD;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompCmmCodeDListRes {
	List<CompCmmCodeD> compCmmCodeDList;
}
