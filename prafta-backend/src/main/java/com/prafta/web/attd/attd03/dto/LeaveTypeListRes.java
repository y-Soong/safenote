package com.prafta.web.attd.attd03.dto;

import java.util.List;

import com.prafta.web.attd.attd03.vo.LeaveType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeaveTypeListRes{
	
	List<LeaveType> leaveTypeList;
}
