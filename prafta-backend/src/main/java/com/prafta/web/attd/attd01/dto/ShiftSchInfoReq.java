package com.prafta.web.attd.attd01.dto;

import java.util.List;

import lombok.Data;

@Data
public class ShiftSchInfoReq{
	ShiftType shiftType;
	List<ShiftPattern> shiftPatternList;
	List<ShiftTeam> shiftTeamList;
	List<ShiftAssign> shiftAssignList;
	
	@Data
	public static class ShiftType {
		String shiftNo;
		String siteCd;
		String shiftPtrnCnt;
		String shiftTeamCnt;
		String shiftCycleDays;
		String useYn;
	}
	
	@Data
	public static class ShiftPattern {
		String siteCd;
		String ptrnIdx;
		String schCd;
	}
	
	@Data
	public static class ShiftTeam {
		String siteCd;
		String teamIdx;
		String teamNm;
	}
	
	@Data
	public static class ShiftAssign {
		String siteCd;
		String dayNo;
		String teamIdx;
		String assignYn;
		String schCd;
	}
}
