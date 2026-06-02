package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.UserResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserWorkPlansResponse {
	List<UserResult> userListResultList;
	
	List<DayResult> dayResultList;
	
	List<SchedResult> schedResultList;
}
