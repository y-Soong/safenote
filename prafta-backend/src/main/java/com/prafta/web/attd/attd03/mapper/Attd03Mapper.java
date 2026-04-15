package com.prafta.web.attd.attd03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd03.application.command.LeaveTypeCommand;
import com.prafta.web.attd.attd03.application.query.LeaveNoDupCheckQuery;
import com.prafta.web.attd.attd03.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd03.result.LeaveTypeResult;
import com.prafta.web.attd.attd03.vo.LeaveNoDupChk;

@Mapper
public interface Attd03Mapper {
	
	void updateLeaveType(LeaveTypeCommand command);
	
	String selectLeaveCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	List<LeaveTypeResult> selectLeaves(LeaveTypeListQuery query);
	
	LeaveNoDupChk selectLeaveNoDupChkResult(LeaveNoDupCheckQuery query);
}
