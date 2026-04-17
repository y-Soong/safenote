package com.prafta.web.attd.attd05.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;
import com.prafta.web.attd.attd05.mapper.Attd05Mapper;
import com.prafta.web.attd.attd05.result.DayResult;
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

//    @Override
//    public AttdStdTimeRuleListResponse getAttdStdTimeRuleList(AttdStdTimeRuleListParam param) {	
//
//        List<AttdStdTimeRuleResult> attdStdTimeRuleResultList = attd04Mapper.selectAttdStdTimeRuleList(AttdStdTimeRuleListQuery.from(param));
//        List<AttdStdTimeRuleHistResult> attdStdTimeRuleHistResultList = attd04Mapper.selectAttdStdTimeRuleHistList(AttdStdTimeRuleListQuery.from(param));
//
//        if (attdStdTimeRuleResultList == null || attdStdTimeRuleResultList.isEmpty()) {
//            return null;
//        }
//
//        return AttdStdTimeRuleListResponse.builder()
//                .attdStdTimeRuleResultList(attdStdTimeRuleResultList)
//                .attdStdTimeRuleHistResultList(attdStdTimeRuleHistResultList)
//                .build();
//    }
//
//    @Override
//    public void saveAttdStdTimeRule(AttdStdTimeRuleParam param) {
//    	String stdTimeRuleType = "";
//    	String stdTimeType = "";
//    	
//    	// 출근 시간 표준화 데이터 저장
//    	stdTimeRuleType = "01";
//    	stdTimeType = param.startStdTimeType();
//
//        attd04Mapper.saveAttdStdTimeRule(AttdStdTimeRuleCommand.from(param, stdTimeRuleType, stdTimeType));
//        
//        // 출퇴근 표준화 데이터 이력 저장
//        int histIdx = attd04Mapper.selectHistIdx(param.gvCmpnyCd());
//        attd04Mapper.saveAttdStdTimeRuleHist(AttdStdTimeRuleHistCommand.from(param, histIdx, stdTimeRuleType, stdTimeType));
//        
//        // 퇴근 시간 표준화 데이터 저장
//        stdTimeRuleType = "02";
//    	stdTimeType = param.endStdTimeType();
//
//        attd04Mapper.saveAttdStdTimeRule(AttdStdTimeRuleCommand.from(param, stdTimeRuleType, stdTimeType));
//        
//        histIdx = attd04Mapper.selectHistIdx(param.gvCmpnyCd());
//        // 출퇴근 표준화 데이터 이력 저장
//        attd04Mapper.saveAttdStdTimeRuleHist(AttdStdTimeRuleHistCommand.from(param, histIdx, stdTimeRuleType, stdTimeType));
//    }
}
