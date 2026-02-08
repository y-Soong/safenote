package com.prafta.web.chkLst.chkLst03.service;

import java.util.Map;

import com.prafta.web.chkLst.chkLst03.dto.InspectResultDetailRequest;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultDetailResponse;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultRequest;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultResponse;

public interface ChkLst03Service {
	InspectResultResponse getChkptInspectItemList(InspectResultRequest requestDto, Map<String, Object> tokenInfo);
	
	InspectResultDetailResponse getChkptInspectAnswerList(InspectResultDetailRequest requestDto, Map<String, Object> tokenInfo);
	
//	List<ChkLst02> selectChkptInspectItemList(ChkLst02ReqDto dto, Map<String, Object> tokenInfo);
//	
//	void updateChkptInspectItemList(List<ChkLst02> dtoList, Map<String, Object> tokenInfo);
//	
//	void deleteChkptInspectItemList(List<ChkLst02> dtoList, Map<String, Object> tokenInfo);
}
