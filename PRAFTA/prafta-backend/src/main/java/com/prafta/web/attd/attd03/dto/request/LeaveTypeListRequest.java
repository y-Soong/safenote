package com.prafta.web.attd.attd03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeListRequest{
	
	private String leaveNo;
	private String leaveNm;
	private String leaveType;
	private String useYn;
}
