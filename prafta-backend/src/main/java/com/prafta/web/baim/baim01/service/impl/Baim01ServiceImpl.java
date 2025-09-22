package com.prafta.web.baim.baim01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.baim.baim01.dto.Baim01;
import com.prafta.web.baim.baim01.dto.Baim01ReqDto;
import com.prafta.web.baim.baim01.mapper.Baim01Mapper;
import com.prafta.web.baim.baim01.service.Baim01Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim01ServiceImpl implements Baim01Service{
	private final Baim01Mapper baim01Mapper;
	
	public Baim01ServiceImpl(Baim01Mapper baim01Mapper) {
		this.baim01Mapper = baim01Mapper;
	}
	
	
	public List<Map<String, Object>> selectSiteInfoList(Baim01ReqDto dto) {
		return baim01Mapper.selectSiteInfoList(dto);
	}
	
//	public int updateUserPw(Baim01ReqDto dto) {
//		return baim01Mapper.updateUserPw(dto);
//	}
	
	public void updateSiteInfo(List<Baim01> dtoList, Map<String, Object> tokenInfo) {		
		for(Baim01 dto : dtoList) {
			baim01Mapper.mergeSiteInfo(dto, tokenInfo);
		}
	}
	
}
