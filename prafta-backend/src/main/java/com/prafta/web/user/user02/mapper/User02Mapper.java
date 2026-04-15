package com.prafta.web.user.user02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.user.user02.application.command.AuthMenuInfoCommand;
import com.prafta.web.user.user02.application.query.AuthMenuListQuery;
import com.prafta.web.user.user02.result.AuthMenuResult;

@Mapper
public interface User02Mapper {
	List<AuthMenuResult> selectAuthMenuList(AuthMenuListQuery query);
	
	void mergeAuthMenuInfo(AuthMenuInfoCommand command);
}
