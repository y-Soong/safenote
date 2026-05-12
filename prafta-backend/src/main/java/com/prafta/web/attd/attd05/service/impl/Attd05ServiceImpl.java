package com.prafta.web.attd.attd05.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd05.application.command.SchTypeCommand;
import com.prafta.web.attd.attd05.application.command.SchTypeDeleCommand;
import com.prafta.web.attd.attd05.application.model.SchTypeDeleModel;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;
import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeDeleParam;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeParam;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd05.application.query.SchListQuery;
import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
import com.prafta.web.attd.attd05.dto.response.LeaveTypeResponse;
import com.prafta.web.attd.attd05.dto.response.SchTypeListResponse;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;
import com.prafta.web.attd.attd05.mapper.Attd05Mapper;
import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.LeaveTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.UserResult;
import com.prafta.web.attd.attd05.service.Attd05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd05ServiceImpl implements Attd05Service {

    private final Attd05Mapper attd05Mapper;
    
    @Override
    public UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param) {
    	
    	UserWorkPlansResponse response = null;
    	
    	List<UserResult> userListResultList = attd05Mapper.selectUserList(UserWorkPlansQuery.from(param));
    	
    	List<DayResult> dayResultList = attd05Mapper.selectDayList(UserWorkPlansQuery.from(param));
    	
    	List<SchedResult> schedResultList = attd05Mapper.selectSchedList(UserWorkPlansQuery.from(param));
    	
    	response = UserWorkPlansResponse.builder()
    									.userListResultList(userListResultList)
    									.dayResultList(dayResultList)
    									.schedResultList(schedResultList)
    									.build();
    	
    	return response;
    }
    
    @Override
    public SchTypeListResponse getSchTypeList(SchTypeListParam param) {
    	
    	SchTypeListResponse response = null;
    	
    	List<SchTypeResult> schTypeResultList = attd05Mapper.selectSchTypeList(SchListQuery.from(param));
    	
    	response = SchTypeListResponse.builder().schTypeResultList(schTypeResultList).build();
    	
    	return response;
    }

    @Override
    public LeaveTypeResponse getLeaveTypeList(LeaveTypeListParam param) {
    	
    	LeaveTypeResponse response= null;
    	
    	List<LeaveTypeResult> leaveTypeResultList = attd05Mapper.selectLeaveTypeList(LeaveTypeListQuery.from(param));
    	
    	response = LeaveTypeResponse.builder().leaveTypeResultList(leaveTypeResultList).build();
    	
    	return response;
    }
    
    @Override
    public void saveUserWorkPlans(SchTypeParam param) {
    	for(SchTypeModel model : param.schTypeModelList()) {
    		attd05Mapper.saveUserWorkPlans(SchTypeCommand.from(model));
    	}
    }
    
    @Override
    public void deleteUserWorkPlans(SchTypeDeleParam param) {
    	for(SchTypeDeleModel model : param.schTypeDeleModelList()) {
    		attd05Mapper.deleteUserWorkPlans(SchTypeDeleCommand.from(model));
    	}
    }
}
