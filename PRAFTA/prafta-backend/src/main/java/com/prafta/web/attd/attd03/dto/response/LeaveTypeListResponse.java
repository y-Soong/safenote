package com.prafta.web.attd.attd03.dto.response;

import java.util.List;

import com.prafta.web.attd.attd03.result.LeaveTypeResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeaveTypeListResponse{
	
	List<LeaveTypeResult> leaveTypeResultList;
}
