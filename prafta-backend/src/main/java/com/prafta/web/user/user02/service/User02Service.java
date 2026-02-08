package com.prafta.web.user.user02.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.user.user02.dto.AuthMenuInfoReq;
import com.prafta.web.user.user02.dto.AuthMenuListReq;
import com.prafta.web.user.user02.dto.AuthMenuListRes;

public interface User02Service {
	AuthMenuListRes selectAuthMenuList(AuthMenuListReq dto, Map<String, Object> tokenInfo);
	
	void updateAuthMenuInfo(List<AuthMenuInfoReq> dtoList, Map<String, Object> tokenInfo);
}
