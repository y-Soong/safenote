package com.prafta.web.baim.baim01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.baim.baim01.application.command.MasterSiteAuthSetCommand;
import com.prafta.web.baim.baim01.application.command.SiteInfoCommand;
import com.prafta.web.baim.baim01.application.command.SiteNodeInfoCommand;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;
import com.prafta.web.baim.baim01.application.param.SiteInfoListParam;
import com.prafta.web.baim.baim01.application.param.SiteInfoParam;
import com.prafta.web.baim.baim01.application.query.SiteInfoListQuery;
import com.prafta.web.baim.baim01.dto.response.SiteInfoListResponse;
import com.prafta.web.baim.baim01.mapper.Baim01Mapper;
import com.prafta.web.baim.baim01.result.SiteInfoResult;
import com.prafta.web.baim.baim01.service.Baim01Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim01ServiceImpl implements Baim01Service{
	private final Baim01Mapper baim01Mapper;
	
	public Baim01ServiceImpl(Baim01Mapper baim01Mapper) {
		this.baim01Mapper = baim01Mapper;
	}
	
	
	public SiteInfoListResponse selectSiteInfoList(SiteInfoListParam param) {
		
		SiteInfoListResponse response = null;
		
		List<SiteInfoResult> siteInfoList = baim01Mapper.selectSiteInfoList(SiteInfoListQuery.from(param));
		
		if(siteInfoList.size() > 0) {
			response = SiteInfoListResponse.builder()
					.siteInfoList(siteInfoList)
					.build();
		}
		
		return response;	
	}
	
	@Transactional
	public void saveSiteInfo(SiteInfoParam param) {
		
		for(SiteInfoModel model : param.siteInfoModelList()) {
			
			String siteCd = "";
			
			if(model.siteCd() != null) {					// 기존 사업장 데이터 변경
				siteCd = model.siteCd();
			} else {										// 신규 사업장 생성
				siteCd = baim01Mapper.selectSiteCd(model.gvCmpnyCd());
			}
			
			// 초기 1 depth 노드 생서
			baim01Mapper.insertSiteNodeInfo(SiteNodeInfoCommand.from(model, siteCd));
			
			baim01Mapper.mergeSiteInfo(SiteInfoCommand.from(model, siteCd));
			
			baim01Mapper.mergeMasterSiteAuthSet(MasterSiteAuthSetCommand.from(model, siteCd));
		}
	}
	
}
