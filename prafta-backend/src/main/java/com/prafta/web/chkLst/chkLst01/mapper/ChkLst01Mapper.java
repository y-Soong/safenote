package com.prafta.web.chkLst.chkLst01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.chkLst.chkLst01.application.command.ChkptInfoCommand;
import com.prafta.web.chkLst.chkLst01.application.query.ChkptListQuery;
import com.prafta.web.chkLst.chkLst01.result.ChkptResult;

@Mapper
public interface ChkLst01Mapper {
	List<ChkptResult> selectChkptList(ChkptListQuery query);
	
	void mergeChkptList(ChkptInfoCommand command);
	
	void updateChkptList(ChkptInfoCommand command);
}
