package com.prafta.web.attd.attd03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeaveTypeListQry{
	
	String leaveNo;
	String leaveNm;
	String leaveType;
	String useYn;
}
