package com.prafta.web.attd.attd01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftSchInfoRequest{
	private ShiftType shiftType;
	private List<ShiftPattern> shiftPatternList;
	private List<ShiftTeam> shiftTeamList;
	private List<ShiftAssign> shiftAssignList;
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ShiftType {
		private String shiftNo;
		private String siteCd;
		private String shiftPtrnCnt;
		private String shiftTeamCnt;
		private String shiftCycleDays;
		private String useYn;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ShiftPattern {
		private String siteCd;
		private String ptrnIdx;
		private String schCd;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ShiftTeam {
		private String siteCd;
		private String teamIdx;
		private String teamNm;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class ShiftAssign {
		private String siteCd;
		private String dayNo;
		private String teamIdx;
		private String assignYn;
		private String schCd;
	}
}
