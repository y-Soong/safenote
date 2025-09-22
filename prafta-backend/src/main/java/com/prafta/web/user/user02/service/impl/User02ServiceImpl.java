package com.prafta.web.user.user02.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.user.user02.dto.User02;
import com.prafta.web.user.user02.dto.User02ReqDto;
import com.prafta.web.user.user02.mapper.User02Mapper;
import com.prafta.web.user.user02.service.User02Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User02ServiceImpl implements User02Service{
	private final User02Mapper user02Mapper;
	
	public User02ServiceImpl(User02Mapper user02Mapper) {
		this.user02Mapper = user02Mapper;
	}
	
	
	public List<Map<String, Object>> selectAuthMenuList(User02ReqDto dto, Map<String, Object> tokenInfo) {
		return user02Mapper.selectAuthMenuList(dto, tokenInfo);
	}

	public void updateAuthMenuInfo(List<User02> dtoList, Map<String, Object> tokenInfo) {
		for(User02 dto : dtoList) {
			user02Mapper.mergeAuthMenuInfo(dto, tokenInfo);
		}
	}
	
}
