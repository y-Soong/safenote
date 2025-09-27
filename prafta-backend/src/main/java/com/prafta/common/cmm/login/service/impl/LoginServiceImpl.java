package com.prafta.common.cmm.login.service.impl;

import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.login.dto.LoginReqDto;
import com.prafta.common.cmm.login.dto.UserJoinReqDto;
import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.exception.LoginFailException;
import com.prafta.common.util.AesGcmUtil;
import com.prafta.common.util.PasswordHashing;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService{
	private final LoginMapper loginMapper;
	
	public LoginServiceImpl(LoginMapper loginMapper) {
		this.loginMapper = loginMapper;
	}
	
	public Map<String, Object> getLoginUser(LoginReqDto dto) {
		Map<String, Object> userInfo = loginMapper.getLoginUser(dto);
		
		if(!PasswordHashing.verifyPassword(dto.getUserPw(), (String)userInfo.get("USER_PW"))) {
			throw new LoginFailException("아이디 혹은 비밀번호를 확인해주세요.");
		}
		
		return loginMapper.getLoginUser(dto);
	}
	
	@Transactional 
	public void insertUserInfo(UserJoinReqDto dto) {
		/* 암호화처리 */
		if(dto.getUserPw() != null) { dto.setUserPw(PasswordHashing.hashPassword(dto.getUserPw())); }
		
		loginMapper.insertUserInfo(dto);
		loginMapper.insertUserSiteAuth(dto);
	}
}

