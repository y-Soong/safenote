package com.prafta.web.attd.attd01.dto;

import java.util.List;

import com.prafta.web.attd.attd01.vo.ShiftAssignInfo;
import com.prafta.web.attd.attd01.vo.ShiftPatternInfo;
import com.prafta.web.attd.attd01.vo.ShiftTeamInfo;
import com.prafta.web.attd.attd01.vo.ShiftTypeInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchDetailRes{
	List<ShiftTypeInfo> shiftTypeInfoList;
	
	List<ShiftPatternInfo> shiftPatternInfoList;
	
	List<ShiftTeamInfo> shiftTeamInfoList;
	
	List<ShiftAssignInfo> shiftAssignInfoList;
}
