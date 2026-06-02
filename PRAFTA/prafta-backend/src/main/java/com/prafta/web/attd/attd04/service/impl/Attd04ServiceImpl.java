package com.prafta.web.attd.attd04.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd04.application.command.AttdStdTimeRuleCommand;
import com.prafta.web.attd.attd04.application.command.AttdStdTimeRuleHistCommand;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleListParam;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleParam;
import com.prafta.web.attd.attd04.application.query.AttdStdTimeRuleListQuery;
import com.prafta.web.attd.attd04.dto.response.AttdStdTimeRuleListResponse;
import com.prafta.web.attd.attd04.mapper.Attd04Mapper;
import com.prafta.web.attd.attd04.result.AttdStdTimeRuleHistResult;
import com.prafta.web.attd.attd04.result.AttdStdTimeRuleResult;
import com.prafta.web.attd.attd04.service.Attd04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd04ServiceImpl implements Attd04Service {

    private final Attd04Mapper attd04Mapper;

    @Override
    public AttdStdTimeRuleListResponse getAttdStdTimeRuleList(AttdStdTimeRuleListParam param) {

        List<AttdStdTimeRuleResult> attdStdTimeRuleResultList = attd04Mapper.selectAttdStdTimeRuleList(AttdStdTimeRuleListQuery.from(param));
        List<AttdStdTimeRuleHistResult> attdStdTimeRuleHistResultList = attd04Mapper.selectAttdStdTimeRuleHistList(AttdStdTimeRuleListQuery.from(param));

        if (attdStdTimeRuleResultList == null || attdStdTimeRuleResultList.isEmpty()) {
            return null;
        }

        return AttdStdTimeRuleListResponse.builder()
                .attdStdTimeRuleResultList(attdStdTimeRuleResultList)
                .attdStdTimeRuleHistResultList(attdStdTimeRuleHistResultList)
                .build();
    }

    @Override
    public void saveAttdStdTimeRule(AttdStdTimeRuleParam param) {
    	String stdTimeRuleType = "";
    	String stdTimeType = "";
    	
    	// 출근 시간 표준화 데이터 저장
    	stdTimeRuleType = "01";
    	stdTimeType = param.startStdTimeType();

        attd04Mapper.saveAttdStdTimeRule(AttdStdTimeRuleCommand.from(param, stdTimeRuleType, stdTimeType));
        
        // 출퇴근 표준화 데이터 이력 저장
        int histIdx = attd04Mapper.selectHistIdx(param.gvCmpnyCd());
        attd04Mapper.saveAttdStdTimeRuleHist(AttdStdTimeRuleHistCommand.from(param, histIdx, stdTimeRuleType, stdTimeType));
        
        // 퇴근 시간 표준화 데이터 저장
        stdTimeRuleType = "02";
    	stdTimeType = param.endStdTimeType();

        attd04Mapper.saveAttdStdTimeRule(AttdStdTimeRuleCommand.from(param, stdTimeRuleType, stdTimeType));
        
        histIdx = attd04Mapper.selectHistIdx(param.gvCmpnyCd());
        // 출퇴근 표준화 데이터 이력 저장
        attd04Mapper.saveAttdStdTimeRuleHist(AttdStdTimeRuleHistCommand.from(param, histIdx, stdTimeRuleType, stdTimeType));
    }
}
