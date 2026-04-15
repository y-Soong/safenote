package com.prafta.web.attd.attd04.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd04.application.command.AttdStdTimeRuleCommand;
import com.prafta.web.attd.attd04.application.command.AttdStdTimeRuleHistCommand;
import com.prafta.web.attd.attd04.application.query.AttdStdTimeRuleListQuery;
import com.prafta.web.attd.attd04.result.AttdStdTimeRuleHistResult;
import com.prafta.web.attd.attd04.result.AttdStdTimeRuleResult;

@Mapper
public interface Attd04Mapper {

    List<AttdStdTimeRuleResult> selectAttdStdTimeRuleList(AttdStdTimeRuleListQuery query);
    
    List<AttdStdTimeRuleHistResult> selectAttdStdTimeRuleHistList(AttdStdTimeRuleListQuery query);

    void saveAttdStdTimeRule(AttdStdTimeRuleCommand command);
    
    int selectHistIdx(@Param("gvCmpnyCd") String gvCmpnyCD);
    
    void saveAttdStdTimeRuleHist(AttdStdTimeRuleHistCommand command);
}
