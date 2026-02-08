package com.prafta.common.cmm.login.service;

import java.util.List;
import java.util.Map;

import com.prafta.common.cmm.login.dto.AuthLogoutReq;
import com.prafta.common.cmm.login.dto.LoginReqDto;
import com.prafta.common.cmm.login.dto.UserJoinReqDto;

public interface LoginService {
	Map<String, Object> getLoginUser(LoginReqDto dto, String clientType); 
	
	int logout(AuthLogoutReq req, Map<String, Object> tokenInfo);
	
	void insertUserInfo(UserJoinReqDto dto);
	
	List<Map<String, Object>> selectUserTermsAgrChk(LoginReqDto dto);
	
	void updateAuthMenuInfo(List<LoginReqDto> dtoList, Map<String, Object> tokenInfo);
}
