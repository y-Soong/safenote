package com.prafta.web.attd.attd07.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.attd.attd07.application.command.DailyAttdDetailDeleteCommand;
import com.prafta.web.attd.attd07.application.command.InsertUserAttdHistsCommand;
import com.prafta.web.attd.attd07.application.command.UpdateUserAttdInfosCommand;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdInfosParam;
import com.prafta.web.attd.attd07.application.query.DailyAttdDetailsQuery;
import com.prafta.web.attd.attd07.application.query.MonthlyAttdListQuery;
import com.prafta.web.attd.attd07.dto.response.AttdRecordListResponse;
import com.prafta.web.attd.attd07.dto.response.DailyAttdDetailsResponse;
import com.prafta.web.attd.attd07.mapper.Attd07Mapper;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.service.Attd07Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd07ServiceImpl implements Attd07Service {

    private final Attd07Mapper attd07Mapper;

    @Override
    public AttdRecordListResponse getMonthlyAttdList(MonthlyAttdListParam param) {

        List<MonthlyAttdListResult> attdRecordResultList = attd07Mapper.selectMonthlyAttdList(MonthlyAttdListQuery.from(param));
        List<MonthlyAttdReqSummaryResult> monthlyAttdReqSummaryResultList = attd07Mapper.selectMonthlyAttdReqSummary(MonthlyAttdListQuery.from(param));

        return AttdRecordListResponse.builder()
                .attdRecordResultList(attdRecordResultList)
                .monthlyAttdReqSummaryResultList(monthlyAttdReqSummaryResultList)
                .build();
    }

    @Override
    @Transactional
    public void updateUserAttdInfos(UpdateUserAttdInfosParam param) {
        for (UpdateUserAttdInfosModel model : param.updateUserAttdInfosModelList()) {
        	String attdId = "";
        	
        	if(model.attdId() != null) {
        		attdId = model.attdId();
        	} else {
        		attdId = attd07Mapper.selectAttdId(model.gvCmpnyCd());
        	}
        	
            attd07Mapper.updateUserAttdInfos(UpdateUserAttdInfosCommand.from(attdId, model));
            
            String histId = attd07Mapper.selectHistId(model.gvCmpnyCd());
            
            attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.from(histId, attdId, model));
        }
    }

    @Override
    public DailyAttdDetailsResponse getDailyAttdDetails(DailyAttdDetailsParam param) {

        DailyAttdDetailsResult dailyAttdDetailsResult = attd07Mapper.selectDailyAttdDetails(DailyAttdDetailsQuery.from(param));

        List<DailyAttdDetailHistoryResult> dailyAttdDetailHistoryResultList = attd07Mapper.selectDailyAttdDetailHistory(DailyAttdDetailsQuery.from(param));
        
        List<MonthlyAttdReqResult> MonthlyAttdReqResultList = attd07Mapper.selectMonthlyAttdReq(DailyAttdDetailsQuery.from(param));

        return DailyAttdDetailsResponse.builder()
                .dailyAttdDetailsResult(dailyAttdDetailsResult)
                .dailyAttdDetailHistoryResultList(dailyAttdDetailHistoryResultList)
                .MonthlyAttdReqResultList(MonthlyAttdReqResultList)
                .build();
    }

    @Override
    @Transactional
    public void dailyAttdDetailDelete(DailyAttdDetailDeleteParam param) {
        DailyAttdDetailDeleteCommand command = DailyAttdDetailDeleteCommand.from(param);

        attd07Mapper.insertDailyAttdDetailDeleteHist(command);
        attd07Mapper.dailyAttdDetailDelete(command);
    }
}
