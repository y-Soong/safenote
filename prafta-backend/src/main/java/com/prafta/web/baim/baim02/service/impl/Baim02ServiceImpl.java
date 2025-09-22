package com.prafta.web.baim.baim02.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.baim.baim02.dto.Baim02;
import com.prafta.web.baim.baim02.dto.Baim02ReqDto;
import com.prafta.web.baim.baim02.mapper.Baim02Mapper;
import com.prafta.web.baim.baim02.service.Baim02Service;
import com.prafta.web.user.user02.dto.User02;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim02ServiceImpl implements Baim02Service{
	private final Baim02Mapper baim02Mapper;
	
	public Baim02ServiceImpl(Baim02Mapper baim02Mapper) {
		this.baim02Mapper = baim02Mapper;
	}
		
	public List<Map<String, Object>> selectCompCmmCodeMList(Baim02ReqDto dto, Map<String, Object> tokenInfo) {
		return baim02Mapper.selectCompCmmCodeMList(dto, tokenInfo);
	}

	public List<Baim02> selectCompCmmCodeDList(Baim02ReqDto dto, Map<String, Object> tokenInfo) {
		return baim02Mapper.selectCompCmmCodeDList(dto, tokenInfo);
	}
	
	public void updateCmmCodeDetailInfo(List<Baim02> dtoList, Map<String, Object> tokenInfo) {
		for(Baim02 dto : dtoList) {
			baim02Mapper.mergeCmmCodeDetailInfo(dto, tokenInfo);
		}
	}
	
	public void deleteCmmCodeDetailInfo(List<Baim02> dtoList, Map<String, Object> tokenInfo) {
		for(Baim02 dto : dtoList) {
			baim02Mapper.deleteCmmCodeDetailInfo(dto, tokenInfo);
		}
	}
	
}
