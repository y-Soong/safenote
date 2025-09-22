package com.prafta.web.chkLst.chkLst01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.baim.baim02.dto.Baim02;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01ReqDto;
import com.prafta.web.chkLst.chkLst01.mapper.ChkLst01Mapper;
import com.prafta.web.chkLst.chkLst01.service.ChkLst01Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChkLst01ServiceImpl implements ChkLst01Service{
	private final ChkLst01Mapper chkLst01Mapper;
	
	public ChkLst01ServiceImpl(ChkLst01Mapper chkLst01Mapper) {
		this.chkLst01Mapper = chkLst01Mapper;
	}
		
	public List<ChkLst01> selectChkptList(ChkLst01ReqDto dto, Map<String, Object> tokenInfo) {
		return chkLst01Mapper.selectChkptList(dto, tokenInfo);
	}
	
	public void updateChkptList(List<ChkLst01> dtoList, Map<String, Object> tokenInfo) {
		for(ChkLst01 dto : dtoList) {
			chkLst01Mapper.mergeChkptList(dto, tokenInfo);
		}
	}
	
	public void deleteChkptList(List<ChkLst01> dtoList, Map<String, Object> tokenInfo) {
		for(ChkLst01 dto : dtoList) {
			chkLst01Mapper.updateChkptList(dto, tokenInfo);
		}
	}
}
