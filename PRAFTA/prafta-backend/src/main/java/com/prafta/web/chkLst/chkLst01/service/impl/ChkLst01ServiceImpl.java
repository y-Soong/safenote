package com.prafta.web.chkLst.chkLst01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.chkLst.chkLst01.application.command.ChkptInfoCommand;
import com.prafta.web.chkLst.chkLst01.application.model.ChkptInfoModel;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptInfoParam;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptListParam;
import com.prafta.web.chkLst.chkLst01.application.query.ChkptListQuery;
import com.prafta.web.chkLst.chkLst01.dto.response.ChkptListResponse;
import com.prafta.web.chkLst.chkLst01.mapper.ChkLst01Mapper;
import com.prafta.web.chkLst.chkLst01.result.ChkptResult;
import com.prafta.web.chkLst.chkLst01.service.ChkLst01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst01ServiceImpl implements ChkLst01Service{
	private final ChkLst01Mapper chkLst01Mapper;
			
	public ChkptListResponse selectChkptList(ChkptListParam param) {
		
		ChkptListResponse response = null;
		
		List<ChkptResult> chkptResultList = chkLst01Mapper.selectChkptList(ChkptListQuery.from(param)); 
		
		if(chkptResultList != null && chkptResultList.size() > 0) {
			response = ChkptListResponse.builder().chkptResultList(chkptResultList).build();
		}
		
		return response;
	}
	
	public void updateChkptList(ChkptInfoParam param) {
		for(ChkptInfoModel model : param.chkptInfoModelList()) {
			chkLst01Mapper.mergeChkptList(ChkptInfoCommand.from(model));
		}
	}
	
	public void deleteChkptList(ChkptInfoParam param) {
		for(ChkptInfoModel model : param.chkptInfoModelList()) {
			chkLst01Mapper.updateChkptList(ChkptInfoCommand.from(model));
		}
	}
}
