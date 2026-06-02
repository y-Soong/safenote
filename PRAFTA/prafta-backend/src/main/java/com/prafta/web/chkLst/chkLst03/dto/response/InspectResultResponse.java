package com.prafta.web.chkLst.chkLst03.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst03.result.InspectResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InspectResultResponse {
	private List<InspectResult> inspectResult;
}
