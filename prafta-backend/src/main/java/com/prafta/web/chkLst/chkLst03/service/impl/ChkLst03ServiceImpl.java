package com.prafta.web.chkLst.chkLst03.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.exception.ChkLstApiException;
import com.prafta.web.chkLst.chkLst03.dto.InspectAnswerQuery;
import com.prafta.web.chkLst.chkLst03.dto.InspectItemSubjQuery;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultDetailRequest;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultDetailResponse;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultQuery;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultRequest;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultResponse;
import com.prafta.web.chkLst.chkLst03.mapper.ChkLst03Mapper;
import com.prafta.web.chkLst.chkLst03.service.ChkLst03Service;
import com.prafta.web.chkLst.chkLst03.vo.InspectAnswer;
import com.prafta.web.chkLst.chkLst03.vo.InspectItemSubj;
import com.prafta.web.chkLst.chkLst03.vo.InspectResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChkLst03ServiceImpl implements ChkLst03Service{
	private final ChkLst03Mapper chkLst03Mapper;
	
	public ChkLst03ServiceImpl(ChkLst03Mapper chkLst03Mapper) {
		this.chkLst03Mapper = chkLst03Mapper;
	}
	
	public InspectResultResponse getChkptInspectItemList(InspectResultRequest requestDto, Map<String, Object> tokenInfo) {
		
		InspectResultQuery reqDto = InspectResultQuery.builder()
									.cmpnyCd(requestDto.getCmpnyCd())
									.userId(requestDto.getUserId())
									.fromDate(requestDto.getFromDate())
									.toDate(requestDto.getToDate())
									.siteCd(requestDto.getSiteCd())
									.chkptNm(requestDto.getChkptNm())
									.chkLstType(requestDto.getChkLstType())
									.build();
		
		InspectResultResponse resDto = null;
		
		List<InspectResult> inspectResultList = chkLst03Mapper.selectChkptInspectItemList(reqDto, tokenInfo);
		
		if(inspectResultList.size() > 0) {
			resDto = InspectResultResponse.builder()
						.inspectResult(inspectResultList)
						.build();
		}
		
		return resDto;
	}
	
	public InspectResultDetailResponse getChkptInspectAnswerList(InspectResultDetailRequest requestDto, Map<String, Object> tokenInfo) {
		InspectItemSubjQuery reqDto = InspectItemSubjQuery.builder()
				.workMonth(requestDto.getWorkMonth())
				.siteCd(requestDto.getSiteCd())
				.chkLstType(requestDto.getChkLstType())
				.chkptCd(requestDto.getChkptCd())
				.build();
		
		InspectResultDetailResponse resDto = null;
		
		List<InspectItemSubj> inspectItemSubjList = chkLst03Mapper.selectInspectItemSubjList(reqDto, tokenInfo);
		
		InspectAnswerQuery reqDto2 = InspectAnswerQuery.builder()
				.workMonth(requestDto.getWorkMonth())
				.siteCd(requestDto.getSiteCd())
				.chkLstType(requestDto.getChkLstType())
				.fileType("001")				// 일일점검 파일타입
				.chkptCd(requestDto.getChkptCd())
				.build();
		
		List<InspectAnswer> inspectAnswerList = chkLst03Mapper.selectInspectAnswerList(reqDto2, tokenInfo);
		
		if(inspectItemSubjList.size() > 0) {
			resDto = InspectResultDetailResponse.builder()
						.inspectItemSubjList(inspectItemSubjList)
						.build();
		} else {
			throw new ChkLstApiException("등록된 점검항목이 없습니다.");
		}
		
		if(inspectAnswerList.size() > 0) {
			resDto = resDto.toBuilder()
						.inspectAnswerList(inspectAnswerList)
						.build();
		}
		
		return resDto;
	}
	
//	public List<ChkLst02> selectChkptInspectItemList(ChkLst02ReqDto dto, Map<String, Object> tokenInfo) {
//		return chkLst01Mapper.selectChkptInspectItemList(dto, tokenInfo);
//	}
//	
//	public void updateChkptInspectItemList(List<ChkLst02> dtoList, Map<String, Object> tokenInfo) {
//		for(ChkLst02 dto : dtoList) {
//			chkLst01Mapper.mergeChkptInspectItemList(dto, tokenInfo);
//		}
//	}
//	
//	public void deleteChkptInspectItemList(List<ChkLst02> dtoList, Map<String, Object> tokenInfo) {
//		for(ChkLst02 dto : dtoList) {
//			chkLst01Mapper.updateChkptInspectItemList(dto, tokenInfo);
//		}
//	}
}
