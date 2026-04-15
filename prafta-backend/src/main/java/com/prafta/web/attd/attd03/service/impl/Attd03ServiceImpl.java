package com.prafta.web.attd.attd03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd03.application.command.LeaveTypeCommand;
import com.prafta.web.attd.attd03.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd03.application.param.LeaveTypeParam;
import com.prafta.web.attd.attd03.application.query.LeaveNoDupCheckQuery;
import com.prafta.web.attd.attd03.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd03.dto.response.LeaveTypeListResponse;
import com.prafta.web.attd.attd03.mapper.Attd03Mapper;
import com.prafta.web.attd.attd03.result.LeaveTypeResult;
import com.prafta.web.attd.attd03.service.Attd03Service;
import com.prafta.web.attd.attd03.vo.LeaveNoDupChk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd03ServiceImpl implements Attd03Service{
	private final Attd03Mapper attd03Mapper;
		
	public Attd03ServiceImpl(Attd03Mapper attd03Mapper) {
		this.attd03Mapper = attd03Mapper;
	}
	
	public void updateLeaveType(LeaveTypeParam param) {
		
		String leaveCd = null;
		
		if(param.leaveCd() != null && param.leaveCd() != "") {
			leaveCd = param.leaveCd();
		} else {
			leaveCd = attd03Mapper.selectLeaveCd(param.gvCmpnyCd());
		}
		
		LeaveNoDupChk leaveNoDupChk = attd03Mapper.selectLeaveNoDupChkResult(LeaveNoDupCheckQuery.from(param));
		
		if(leaveNoDupChk != null) {
			throw new ApiException(AttdErrorCode.ATTD_400_001);
		}
		
		attd03Mapper.updateLeaveType(LeaveTypeCommand.from(param));
		
	}
	
	public LeaveTypeListResponse getLeaves(LeaveTypeListParam param) {
		
		LeaveTypeListResponse leaveTypeListRes = null;
		
		List<LeaveTypeResult> leaveTypeResultList = attd03Mapper.selectLeaves(LeaveTypeListQuery.from(param));
		
		if(leaveTypeResultList != null && leaveTypeResultList.size() > 0) {
			leaveTypeListRes = LeaveTypeListResponse.builder()
												.leaveTypeResultList(leaveTypeResultList)
												.build();
		}
		
		return leaveTypeListRes;
	}
}
