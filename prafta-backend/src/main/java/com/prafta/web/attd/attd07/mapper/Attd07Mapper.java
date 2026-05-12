package com.prafta.web.attd.attd07.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd07.application.command.DailyAttdDetailDeleteCommand;
import com.prafta.web.attd.attd07.application.command.InsertUserAttdHistsCommand;
import com.prafta.web.attd.attd07.application.command.UpdateUserAttdInfosCommand;
import com.prafta.web.attd.attd07.application.query.DailyAttdDetailsQuery;
import com.prafta.web.attd.attd07.application.query.MonthlyAttdListQuery;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;

@Mapper
public interface Attd07Mapper {

    List<MonthlyAttdListResult> selectMonthlyAttdList(MonthlyAttdListQuery query);
    
    List<MonthlyAttdReqSummaryResult> selectMonthlyAttdReqSummary(MonthlyAttdListQuery query);
    
    String selectAttdId(@Param("gvCmpnyCd")	String gvCmpnyCd);
    
    String selectHistId(@Param("gvCmpnyCd") String gvCmpnyCd);

    void updateUserAttdInfos(UpdateUserAttdInfosCommand command);
    
    void insertUserAttdInfos(InsertUserAttdHistsCommand command);

    DailyAttdDetailsResult selectDailyAttdDetails(DailyAttdDetailsQuery query);

    List<DailyAttdDetailHistoryResult> selectDailyAttdDetailHistory(DailyAttdDetailsQuery query);
    
    List<MonthlyAttdReqResult> selectMonthlyAttdReq(DailyAttdDetailsQuery query);

    void dailyAttdDetailDelete(DailyAttdDetailDeleteCommand command);

    void insertDailyAttdDetailDeleteHist(DailyAttdDetailDeleteCommand command);
}
