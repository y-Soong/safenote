package com.prafta.web.user.user02.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.user.user02.application.command.AuthMenuInfoCommand;
import com.prafta.web.user.user02.application.model.AuthMenuInfoModel;
import com.prafta.web.user.user02.application.param.AuthMenuInfoParam;
import com.prafta.web.user.user02.application.param.AuthMenuListParam;
import com.prafta.web.user.user02.application.query.AuthMenuListQuery;
import com.prafta.web.user.user02.dto.response.AuthMenuListResponse;
import com.prafta.web.user.user02.mapper.User02Mapper;
import com.prafta.web.user.user02.result.AuthMenuResult;
import com.prafta.web.user.user02.service.User02Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User02ServiceImpl implements User02Service{
	private final User02Mapper user02Mapper;
	
	public User02ServiceImpl(User02Mapper user02Mapper) {
		this.user02Mapper = user02Mapper;
	}
	
	
	public AuthMenuListResponse selectAuthMenuList(AuthMenuListParam param) {
		
		AuthMenuListResponse retDto = null;
		
		List<AuthMenuResult> authMenuList = user02Mapper.selectAuthMenuList(AuthMenuListQuery.from(param));
		
		if(authMenuList.size() > 0) {
			retDto = AuthMenuListResponse.builder()
					.authMenuList(authMenuList)
					.build();
		}
		
		return retDto;
	}

	public void updateAuthMenuInfo(AuthMenuInfoParam param) {
		
		for(AuthMenuInfoModel model : param.authMenuInfoModelList()) {
			
			user02Mapper.mergeAuthMenuInfo(AuthMenuInfoCommand.from(model));
		}
	}
	
}
