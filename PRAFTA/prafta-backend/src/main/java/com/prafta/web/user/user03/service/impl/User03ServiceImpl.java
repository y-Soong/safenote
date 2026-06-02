package com.prafta.web.user.user03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.user.user03.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user03.application.model.UserSiteAuthModel;
import com.prafta.web.user.user03.application.param.SiteInfoListParam;
import com.prafta.web.user.user03.application.param.UserSiteAuthParam;
import com.prafta.web.user.user03.application.query.SiteInfoListQuery;
import com.prafta.web.user.user03.dto.response.SiteInfoListResponse;
import com.prafta.web.user.user03.mapper.User03Mapper;
import com.prafta.web.user.user03.result.SiteInfoResult;
import com.prafta.web.user.user03.service.User03Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User03ServiceImpl implements User03Service{
	private final User03Mapper user03Mapper;
	
	public User03ServiceImpl(User03Mapper user03Mapper) {
		this.user03Mapper = user03Mapper;
	}
	
	
	public SiteInfoListResponse selectSiteInfoSearch(SiteInfoListParam param) {
		
		SiteInfoListResponse response = null;
		
		List<SiteInfoResult> siteInfoList = user03Mapper.selectSiteInfoSearch(SiteInfoListQuery.from(param));
		
		if(siteInfoList != null && siteInfoList.size() > 0) {
			response = SiteInfoListResponse.builder()
									.siteInfoList(siteInfoList)
									.build();
		}
		
		return response;
	}

	public void updateUserSiteAuth(UserSiteAuthParam param) {
		for(UserSiteAuthModel model : param.userSiteAuthModelList()) {
			user03Mapper.mergeUserSiteAuth(UserSiteAuthCommand.from(model));
		}
	}
	
}
