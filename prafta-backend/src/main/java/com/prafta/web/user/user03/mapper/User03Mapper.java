package com.prafta.web.user.user03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.user.user03.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user03.application.query.SiteInfoListQuery;
import com.prafta.web.user.user03.result.SiteInfoResult;

@Mapper
public interface User03Mapper {
	List<SiteInfoResult> selectSiteInfoSearch(SiteInfoListQuery query);
	
	void mergeUserSiteAuth(UserSiteAuthCommand command);
}
