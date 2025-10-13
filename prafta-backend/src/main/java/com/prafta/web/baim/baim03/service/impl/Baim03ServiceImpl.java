package com.prafta.web.baim.baim03.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.exception.CmmApiException;
import com.prafta.web.baim.baim03.dto.Baim03;
import com.prafta.web.baim.baim03.dto.Baim03ReqDto;
import com.prafta.web.baim.baim03.mapper.Baim03Mapper;
import com.prafta.web.baim.baim03.service.Baim03Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim03ServiceImpl implements Baim03Service{
	private final Baim03Mapper baim03Mapper;
	
	public Baim03ServiceImpl(Baim03Mapper baim03Mapper) {
		this.baim03Mapper = baim03Mapper;
	}
		
	public List<Baim03> selectTermsList(Baim03ReqDto dto, Map<String, Object> tokenInfo) {
		return baim03Mapper.selectTermsList(dto, tokenInfo);
	}

	public List<Baim03> selectTermsDList(Baim03ReqDto dto, Map<String, Object> tokenInfo) {
		return baim03Mapper.selectTermsDList(dto, tokenInfo);
	}
	
	public Baim03 selectTermsInfo(Baim03ReqDto dto, Map<String, Object> tokenInfo) {
		return baim03Mapper.selectTermsInfo(dto, tokenInfo);
	}
	
	@Transactional
	public void updateTermsInfo(Baim03 dto, Map<String, Object> tokenInfo) {
		Baim03ReqDto reqDto = new Baim03ReqDto();
		reqDto.setSr_termsId(dto.getTermsId());
		List<Baim03> retList = baim03Mapper.selectTermsList(reqDto, tokenInfo);
		
		if(retList.size() != 1) {
			throw new CmmApiException("약관 데이터 생성 오류 !\n관리자에게 문의해주세요.");
		}
		
		String versionNo = String.valueOf(Integer.parseInt(retList.get(retList.size() - 1).getTermsVersion()) + 1);
		
		System.out.println(versionNo);
		dto.setTermsVersion(versionNo); 
		baim03Mapper.mergeTermsInfo(dto, tokenInfo);
		baim03Mapper.insertTermsIdVersionInfo(dto, tokenInfo);
	}
	
	public void deleteCmmCodeDetailInfo(List<Baim03> dtoList, Map<String, Object> tokenInfo) {
		for(Baim03 dto : dtoList) {
			baim03Mapper.deleteCmmCodeDetailInfo(dto, tokenInfo);
		}
	}
	
}
