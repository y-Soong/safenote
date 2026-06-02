package com.prafta.web.baim.baim02.dto.response;

import java.util.List;

import com.prafta.web.baim.baim02.result.CompCmmCodeDResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompCmmCodeDListResponse {
	List<CompCmmCodeDResult> compCmmCodeDList;
}
