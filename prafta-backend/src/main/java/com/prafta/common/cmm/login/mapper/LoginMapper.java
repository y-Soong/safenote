package com.prafta.common.cmm.login.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.login.dto.LoginReqDto;
import com.prafta.common.cmm.login.dto.UserJoinReqDto;
import com.prafta.web.baim.baim03.dto.Baim03;

@Mapper
public interface LoginMapper {
	Map<String, Object> getLoginUser(LoginReqDto dto);
	
	int insertUserInfo(UserJoinReqDto dto);
	
	int insertUserSiteAuth(UserJoinReqDto dto);
	
	List<Map<String, Object>> selectRequiredTermsList();
	
	int insertTermsUserAgrMgmt(Map<String, Object> param);
	
	List<Map<String, Object>> selectUserTermsAgrChk(LoginReqDto dto);
	
	void mergeAuthMenuInfo(@Param(value = "param") LoginReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
