package com.prafta.web.attd.attd05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd05.application.command.SchTypeCommand;
import com.prafta.web.attd.attd05.application.command.SchTypeDeleCommand;
import com.prafta.web.attd.attd05.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd05.application.query.SchListQuery;
import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.LeaveTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.UserResult;

@Mapper
public interface Attd05Mapper {
	
	List<UserResult> selectUserList(UserWorkPlansQuery query);
	
	List<DayResult> selectDayList(UserWorkPlansQuery query);
	
	List<SchedResult> selectSchedList(UserWorkPlansQuery query);
	
	List<SchTypeResult> selectSchTypeList(SchListQuery query);
	
	List<LeaveTypeResult> selectLeaveTypeList(LeaveTypeListQuery query);
	
	void saveUserWorkPlans(SchTypeCommand command);
	
	void deleteUserWorkPlans(SchTypeDeleCommand command);
}
