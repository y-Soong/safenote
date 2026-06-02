package com.prafta.web.attd.attd01.dto.response;

import java.util.List;

import com.prafta.web.attd.attd01.result.ShiftAssignInfoResult;
import com.prafta.web.attd.attd01.result.ShiftPatternInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTypeInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchDetailResponse{
	private List<ShiftTypeInfoResult> shiftTypeInfoResultList;
	
	private List<ShiftPatternInfoResult> shiftPatternInfoResultList;
	
	private List<ShiftTeamInfoResult> shiftTeamInfoResultList;
	
	private List<ShiftAssignInfoResult> shiftAssignInfoResultList;
}
