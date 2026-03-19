package com.prafta.web.baim.baim01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.baim.baim01.dto.MasterSiteAuthSetCommand;
import com.prafta.web.baim.baim01.dto.SiteInfoListQry;
import com.prafta.web.baim.baim01.dto.SiteInfoListReq;
import com.prafta.web.baim.baim01.dto.SiteInfoListRes;
import com.prafta.web.baim.baim01.dto.SiteInfoReq;
import com.prafta.web.baim.baim01.dto.SiteInfoSave;
import com.prafta.web.baim.baim01.mapper.Baim01Mapper;
import com.prafta.web.baim.baim01.service.Baim01Service;
import com.prafta.web.baim.baim01.vo.SiteInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim01ServiceImpl implements Baim01Service{
	private final Baim01Mapper baim01Mapper;
	
	public Baim01ServiceImpl(Baim01Mapper baim01Mapper) {
		this.baim01Mapper = baim01Mapper;
	}
	
	
	public SiteInfoListRes selectSiteInfoList(SiteInfoListReq dto, Map<String, Object> tokenInfo) {
		
		SiteInfoListQry reqDto = SiteInfoListQry.builder()
								.siteCd(dto.getSiteCd())
								.siteNo(dto.getSiteNo())
								.siteNm(dto.getSiteNm())
								.build();
		
		SiteInfoListRes retDto = null;
		
		List<SiteInfo> siteInfoList = baim01Mapper.selectSiteInfoList(reqDto, tokenInfo);
		
		if(siteInfoList.size() > 0) {
			retDto = SiteInfoListRes.builder()
					.siteInfoList(siteInfoList)
					.build();
		}
		
		return retDto;	
	}
	
	@Transactional
	public void saveSiteInfo(List<SiteInfoReq> dtoList, Map<String, Object> tokenInfo) {
		
		for(SiteInfoReq dto : dtoList) {
			
			String siteCd = "";
			
			if(dto.getSiteCd() != null) {
				siteCd = dto.getSiteCd();
			} else {
				siteCd = baim01Mapper.selectSiteCd(tokenInfo);
			}
			
			SiteInfoSave siteInfoSave = SiteInfoSave.builder()
									    .cmpnyCd(dto.getCmpnyCd())
									    .siteCd(siteCd)
									    .siteNo(dto.getSiteNo())
									    .siteNm(dto.getSiteNm())
					
									    .addr1(dto.getAddr1())
									    .addr2(dto.getAddr2())
									    .zipCode(dto.getZipCode())
					
									    .strDate(dto.getStrDate())   // 시작일
									    .endDate(dto.getEndDate())   // 종료일
					
									    .useYn(dto.getUseYn())
									    .siteAdminId(dto.getSiteAdminId())
									    .telNo(dto.getTelNo())
					
									    .gpsRange(dto.getGpsRange())
									    .siteDesc(dto.getSiteDesc())
									    .build();
			
			baim01Mapper.mergeSiteInfo(siteInfoSave, tokenInfo);
			
			MasterSiteAuthSetCommand masterSiteAuthSetCommand = MasterSiteAuthSetCommand.builder()
																						.siteCd(siteCd)
																						.build();
			
			baim01Mapper.mergeMasterSiteAuthSet(masterSiteAuthSetCommand, tokenInfo);
			
			
		}
	}
	
}
