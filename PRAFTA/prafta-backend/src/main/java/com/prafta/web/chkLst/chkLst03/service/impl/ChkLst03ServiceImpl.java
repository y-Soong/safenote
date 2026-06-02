package com.prafta.web.chkLst.chkLst03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.chkLst.ChkLstErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultDetailParam;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultParam;
import com.prafta.web.chkLst.chkLst03.application.query.InspectAnswerQuery;
import com.prafta.web.chkLst.chkLst03.application.query.InspectItemSubjQuery;
import com.prafta.web.chkLst.chkLst03.application.query.InspectResultQuery;
import com.prafta.web.chkLst.chkLst03.dto.response.InspectResultDetailResponse;
import com.prafta.web.chkLst.chkLst03.dto.response.InspectResultResponse;
import com.prafta.web.chkLst.chkLst03.mapper.ChkLst03Mapper;
import com.prafta.web.chkLst.chkLst03.result.InspectAnswerResult;
import com.prafta.web.chkLst.chkLst03.result.InspectItemSubjResult;
import com.prafta.web.chkLst.chkLst03.result.InspectResult;
import com.prafta.web.chkLst.chkLst03.service.ChkLst03Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChkLst03ServiceImpl implements ChkLst03Service{
	private final ChkLst03Mapper chkLst03Mapper;
	
	public ChkLst03ServiceImpl(ChkLst03Mapper chkLst03Mapper) {
		this.chkLst03Mapper = chkLst03Mapper;
	}
	
	public InspectResultResponse getChkptInspectItemList(InspectResultParam param) {
				
		InspectResultResponse response = null;
		
		List<InspectResult> inspectResultList = chkLst03Mapper.selectChkptInspectItemList(InspectResultQuery.from(param));
		
		if(inspectResultList.size() > 0) {
			response = InspectResultResponse.builder()
						.inspectResult(inspectResultList)
						.build();
		}
		
		return response;
	}
	
	public InspectResultDetailResponse getChkptInspectAnswerList(InspectResultDetailParam param) {
		
		InspectResultDetailResponse response = null;
		
		List<InspectItemSubjResult> inspectItemSubjResultList = chkLst03Mapper.selectInspectItemSubjList(InspectItemSubjQuery.from(param));
				
		List<InspectAnswerResult> inspectAnswerResultList = chkLst03Mapper.selectInspectAnswerList(InspectAnswerQuery.from(param));
		
		response = InspectResultDetailResponse.builder()
												.inspectItemSubjResultList(inspectItemSubjResultList)
												.inspectAnswerResultList(inspectAnswerResultList)
												.build();
		
		return response;
	}
}
