package com.prafta.common.cmm.login.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.login.dto.ActiveToken;
import com.prafta.common.cmm.login.dto.LoginReqDto;
import com.prafta.common.cmm.login.dto.UserJoinReqDto;
import com.prafta.common.cmm.login.dto.UserLogout;
import com.prafta.common.cmm.login.dto.UserRowLock;

@Mapper
public interface LoginMapper {
	Map<String, Object> getLoginUser(LoginReqDto dto);
	
	void lockUserRow(UserRowLock dto);
	
	void revokeActiveToken(ActiveToken dto);
	
	void insertAuthToken(ActiveToken dto);
	
	int revokeActiveTokenByClient(UserLogout dto);

    int revokeAllActiveTokens(UserLogout dto);
	
	int insertUserInfo(UserJoinReqDto dto);
	
	int insertUserSiteAuth(UserJoinReqDto dto);
	
	List<Map<String, Object>> selectRequiredTermsList();
	
	int insertTermsUserAgrMgmt(Map<String, Object> param);
	
	List<Map<String, Object>> selectUserTermsAgrChk(LoginReqDto dto);
	
	void mergeAuthMenuInfo(@Param(value = "param") LoginReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
