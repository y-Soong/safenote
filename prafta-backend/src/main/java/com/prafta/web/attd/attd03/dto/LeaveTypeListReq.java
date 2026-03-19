package com.prafta.web.attd.attd03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeaveTypeListReq{
	
	String leaveNo;
	String leaveNm;
	String leaveType;
	String useYn;
}
