package com.prafta.web.chkLst.chkLst03.dto;

import java.util.List;

import com.prafta.web.chkLst.chkLst03.vo.InspectResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InspectResultResponse {
	List<InspectResult> inspectResult;

}
