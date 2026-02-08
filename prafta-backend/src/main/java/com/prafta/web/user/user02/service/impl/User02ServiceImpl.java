package com.prafta.web.user.user02.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.user.user02.dto.AuthMenuInfoQry;
import com.prafta.web.user.user02.dto.AuthMenuInfoReq;
import com.prafta.web.user.user02.dto.AuthMenuListQry;
import com.prafta.web.user.user02.dto.AuthMenuListReq;
import com.prafta.web.user.user02.dto.AuthMenuListRes;
import com.prafta.web.user.user02.mapper.User02Mapper;
import com.prafta.web.user.user02.service.User02Service;
import com.prafta.web.user.user02.vo.AuthMenu;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User02ServiceImpl implements User02Service{
	private final User02Mapper user02Mapper;
	
	public User02ServiceImpl(User02Mapper user02Mapper) {
		this.user02Mapper = user02Mapper;
	}
	
	
	public AuthMenuListRes selectAuthMenuList(AuthMenuListReq dto, Map<String, Object> tokenInfo) {
		
		AuthMenuListQry reqDto = AuthMenuListQry.builder()
				.menuDNm(dto.getMenuDNm())
				.menuMNm(dto.getMenuMNm())
				.authCd(dto.getAuthCd())
				.useYn(dto.getUseYn())
				.build();
		
		AuthMenuListRes retDto = null;
		
		List<AuthMenu> authMenuList = user02Mapper.selectAuthMenuList(reqDto, tokenInfo);
		
		if(authMenuList.size() > 0) {
			retDto = AuthMenuListRes.builder()
					.authMenuList(authMenuList)
					.build();
		}
		
		return retDto;
	}

	public void updateAuthMenuInfo(List<AuthMenuInfoReq> dtoList, Map<String, Object> tokenInfo) {
		
		for(AuthMenuInfoReq dto : dtoList) {
			
			AuthMenuInfoQry reqDto = AuthMenuInfoQry.builder()
					.authCd(dto.getAuthCd())
					.menuDId(dto.getMenuDId())
					.useYn(dto.getUseYn())
					.btnSrch(dto.getBtnSrch())
					.btnNew(dto.getBtnNew())
					.btnDel(dto.getBtnDel())
					.btnSave(dto.getBtnSave())
					.btnExcl(dto.getBtnExcl())
					.build();
			
			user02Mapper.mergeAuthMenuInfo(reqDto, tokenInfo);
		}
	}
	
}
