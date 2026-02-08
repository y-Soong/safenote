package com.prafta.web.risk.risk01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.exception.RiskApiException;
import com.prafta.web.risk.risk01.dto.RiskHazardListQry;
import com.prafta.web.risk.risk01.dto.RiskHazardListReq;
import com.prafta.web.risk.risk01.dto.RiskHazardListRes;
import com.prafta.web.risk.risk01.dto.RiskHazardQry;
import com.prafta.web.risk.risk01.dto.RiskHazardReq;
import com.prafta.web.risk.risk01.dto.RiskTypeListQry;
import com.prafta.web.risk.risk01.dto.RiskTypeListReq;
import com.prafta.web.risk.risk01.dto.RiskTypeListRes;
import com.prafta.web.risk.risk01.dto.RiskTypeQry;
import com.prafta.web.risk.risk01.dto.RiskTypeReq;
import com.prafta.web.risk.risk01.mapper.Risk01Mapper;
import com.prafta.web.risk.risk01.service.Risk01Service;
import com.prafta.web.risk.risk01.vo.RiskHazard;
import com.prafta.web.risk.risk01.vo.RiskType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Risk01ServiceImpl implements Risk01Service{
	private final Risk01Mapper risk01Mapper;
	
	public Risk01ServiceImpl(Risk01Mapper risk01Mapper) {
		this.risk01Mapper = risk01Mapper;
	}
	
	public RiskTypeListRes selectRiskTypeList(RiskTypeListReq dto, Map<String, Object> tokenInfo) {
		
		RiskTypeListQry reqDto = RiskTypeListQry.builder()
				.processCd(dto.getProcessCd())
				.siteCd(dto.getSiteCd())
				.riskTypeNm(dto.getRiskTypeNm())
				.useYn(dto.getUseYn())
				.build();
		
		RiskTypeListRes retDto = null;
		
		List<RiskType> riskTypeList = risk01Mapper.selectRiskTypeList(reqDto, tokenInfo);
		
		if(riskTypeList.size() > 0) {
			retDto = RiskTypeListRes.builder()
					.riskTypeList(riskTypeList)
					.build();
		}
		
		return retDto;
	}

	public void updateRistType(List<RiskTypeReq> dtoList, Map<String, Object> tokenInfo) {
		for(RiskTypeReq dto : dtoList) {
			
			RiskTypeQry reqDto = RiskTypeQry.builder()
					.cmpnyCd(dto.getCmpnyCd())
					.processCd(dto.getProcessCd())
					.riskTypeCd(dto.getRiskTypeCd())
					.riskTypeNm(dto.getRiskTypeNm())
					.siteCd(dto.getSiteCd())
					.useYn(dto.getUseYn())
					.riskTypeDesc(dto.getRiskTypeDesc())
					.build();
			
			risk01Mapper.mergeRistType(reqDto, tokenInfo);
		}
	}
	
	public void deleteRistType(List<RiskTypeReq> dtoList, Map<String, Object> tokenInfo) {
		for(RiskTypeReq dto : dtoList) {
			
			RiskTypeQry reqDto = RiskTypeQry.builder()
					.cmpnyCd(dto.getCmpnyCd())
					.processCd(dto.getProcessCd())
					.riskTypeCd(dto.getRiskTypeCd())
					.riskTypeNm(dto.getRiskTypeNm())
					.siteCd(dto.getSiteCd())
					.useYn(dto.getUseYn())
					.riskTypeDesc(dto.getRiskTypeDesc())
					.build();
			
			int riskHazardCnt = risk01Mapper.selectRiskHazardCnt(reqDto);
			
			if(riskHazardCnt > 0) {
				throw new RiskApiException("위험요인 구분 하위에 유해요인이 존재합니다.\n확인 후 다시 시도해주세요.");
			}
			
			risk01Mapper.deleteRistType(reqDto, tokenInfo);
		}
	}
	
	public RiskHazardListRes selectRiskHazardList(RiskHazardListReq dto, Map<String, Object> tokenInfo) {
		
		RiskHazardListQry reqDto = RiskHazardListQry.builder()
				.riskTypeCd(dto.getRiskTypeCd())
				.hazardNm(dto.getHazardNm())
				.hazardDesc(dto.getHazardDesc())
				.build();
		
		RiskHazardListRes retDto = null;
		
		List<RiskHazard> riskHazardList = risk01Mapper.selectRiskHazardList(reqDto, tokenInfo);
		
		if(riskHazardList.size() > 0) {
			retDto = RiskHazardListRes.builder()
					.riskHazardList(riskHazardList)
					.build();
		}
		
		return retDto;
	}
	
	public void updateRiskHazard(List<RiskHazardReq> dtoList, Map<String, Object> tokenInfo) {
		for(RiskHazardReq dto : dtoList) {
			
			RiskHazardQry reqDto = RiskHazardQry.builder()
					.cmpnyCd(dto.getCmpnyCd())
					.riskTypeCd(dto.getRiskTypeCd())
					.hazardCd(dto.getHazardCd())
					.hazardNm(dto.getHazardNm())
					.siteCd(dto.getSiteCd())
					.hazardDesc(dto.getHazardDesc())
					.build();
			
			risk01Mapper.mergeRiskHazard(reqDto, tokenInfo);
		}
	}
	
	public void deleteRiskHazard(List<RiskHazardReq> dtoList, Map<String, Object> tokenInfo) {
		for(RiskHazardReq dto : dtoList) {
			
			RiskHazardQry reqDto = RiskHazardQry.builder()
					.cmpnyCd(dto.getCmpnyCd())
					.riskTypeCd(dto.getRiskTypeCd())
					.hazardCd(dto.getHazardCd())
					.hazardNm(dto.getHazardNm())
					.siteCd(dto.getSiteCd())
					.hazardDesc(dto.getHazardDesc())
					.build();
			
			risk01Mapper.deleteRiskHazard(reqDto, tokenInfo);
		}
	}
}
