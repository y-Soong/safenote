package com.prafta.web.user.user01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.user.user01.dto.User01;
import com.prafta.web.user.user01.dto.User01ReqDto;
import com.prafta.web.user.user01.mapper.User01Mapper;
import com.prafta.web.user.user01.service.User01Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User01ServiceImpl implements User01Service{
	private final User01Mapper user01Mapper;
	
	public User01ServiceImpl(User01Mapper user01Mapper) {
		this.user01Mapper = user01Mapper;
	}
	
	
	public List<Map<String, Object>> selectUserInfoList(User01ReqDto dto) {
		return user01Mapper.selectUserInfoList(dto);
	}
	
	public int updateUserPw(User01ReqDto dto) {
		return user01Mapper.updateUserPw(dto);
	}
	
	@Transactional
	public void updateUserInfo(List<User01> dtoList, Map<String, Object> tokenInfo) {
		for(User01 dto : dtoList) {
			
			User01ReqDto reqDto = new User01ReqDto();
			reqDto.setSr_cmpnyCd(dto.getCmpnyCd());
			reqDto.setSr_userId(dto.getUserId());
			reqDto.setSr_userNm(dto.getUserNm());
			reqDto.setSr_useYn(dto.getUseYn());
			reqDto.setSr_siteCd(dto.getSiteCd());
//			User01ReqDto reqDto = User01ReqDto.builder()
//					.cmpnyCd(dto.getCmpnyCd())
//					.userId(dto.getUserId())
//					.userNm(dto.getUserNm())
//					.useYn(dto.getUseYn())
//					.siteCd(dto.getSiteCd())
//					.build();
			
			Map<String, Object> userInfo = user01Mapper.selectUserSiteInfo(reqDto);
			
			if(!dto.getSiteCd().equals(userInfo.get("SITE_CD"))) {
				user01Mapper.deleteUserSiteAuth(dto, tokenInfo);
				user01Mapper.insertUserSiteAuth(dto, tokenInfo);
			}
				
			user01Mapper.mergeUserInfo(dto, tokenInfo);
		}
	}
	
}
