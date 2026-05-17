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
import com.prafta.web.attd.attd05.result.SchTypeUseYnResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.UserResult;

@Mapper
public interface Attd05Mapper {
	
	List<UserResult> selectUserList(UserWorkPlansQuery query);
	
	List<DayResult> selectDayList(UserWorkPlansQuery query);
	
	List<SchedResult> selectSchedList(UserWorkPlansQuery query);
	
	List<SchTypeResult> selectSchTypeList(SchListQuery query);

	/**
	 * 근무타입(SCH_CD)별 effective-dating 버전 목록.
	 * TB_SCH_MGMT(현재본) + TB_SCH_MGMT_HIST(이력본) 합집합을
	 * 동일 SCH_CD+APPLY_DATE 중복 시 MAX(HIST_IDX) 1건으로 정리하여 반환한다.
	 * USE_YN='N' 버전도 차단 근거로 살려야 하므로 USE_YN 필터를 걸지 않는다.
	 */
	List<SchTypeUseYnResult> selectSchTypeUseYnList(SchListQuery query);

	List<LeaveTypeResult> selectLeaveTypeList(LeaveTypeListQuery query);
	
	void saveUserWorkPlans(SchTypeCommand command);
	
	void deleteUserWorkPlans(SchTypeDeleCommand command);
}
