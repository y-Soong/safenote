package com.prafta.web.user.user02.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.user.user02.dto.User02;
import com.prafta.web.user.user02.dto.User02ReqDto;

public interface User02Service {
	List<Map<String, Object>> selectAuthMenuList(User02ReqDto dto, Map<String, Object> tokenInfo);
	
	void updateAuthMenuInfo(List<User02> dtoList, Map<String, Object> tokenInfo);
}
