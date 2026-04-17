package com.prafta.web.attd.attd05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.UserResult;

@Mapper
public interface Attd05Mapper {
	
	List<UserResult> selectUserList(UserWorkPlansQuery query);
	
	List<DayResult> selectDayList(UserWorkPlansQuery query);
	
	List<SchedResult> selectSchedList(UserWorkPlansQuery query);

//    List<AttdStdTimeRuleResult> selectAttdStdTimeRuleList(AttdStdTimeRuleListQuery query);
//    
//    List<AttdStdTimeRuleHistResult> selectAttdStdTimeRuleHistList(AttdStdTimeRuleListQuery query);
//
//    void saveAttdStdTimeRule(AttdStdTimeRuleCommand command);
//    
//    int selectHistIdx(@Param("gvCmpnyCd") String gvCmpnyCD);
//    
//    void saveAttdStdTimeRuleHist(AttdStdTimeRuleHistCommand command);
}
