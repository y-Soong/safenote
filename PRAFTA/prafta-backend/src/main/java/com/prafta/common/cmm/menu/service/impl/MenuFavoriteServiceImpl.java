package com.prafta.common.cmm.menu.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.menu.application.param.MenuFavoriteListParam;
import com.prafta.common.cmm.menu.application.param.MenuFavoriteToggleParam;
import com.prafta.common.cmm.menu.dto.response.MenuFavoriteListResponse;
import com.prafta.common.cmm.menu.dto.response.MenuFavoriteToggleResponse;
import com.prafta.common.cmm.menu.mapper.MenuFavoriteMapper;
import com.prafta.common.cmm.menu.service.MenuFavoriteService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuFavoriteServiceImpl implements MenuFavoriteService {

	private final MenuFavoriteMapper menuFavoriteMapper;

	@Override
	public MenuFavoriteListResponse selectMyFavorites(MenuFavoriteListParam param) {

		List<String> favoriteMenuDIds =
				menuFavoriteMapper.selectFavoriteMenuDIds(param.cmpnyCd(), param.userCd());

		return MenuFavoriteListResponse.builder()
				.favoriteMenuDIds(favoriteMenuDIds)
				.build();
	}

	@Override
	@Transactional
	public MenuFavoriteToggleResponse toggleFavorite(MenuFavoriteToggleParam param) {

		// 1) MENU_D_ID 유효성 검증 — 웹 메뉴(menuSrc='001')로 실재해야 한다.
		//    임의 문자열 별표(쓰레기 행 적재) 방지.
		if (menuFavoriteMapper.countMenuDExists(param.menuDId()) == 0) {
			log.info("즐겨찾기 토글 거부 - 존재하지 않는 메뉴 (cmpnyCd={}, userCd={}, menuDId={})",
					param.cmpnyCd(), param.userCd(), param.menuDId());
			throw new ApiException(CommonErrorCode.COMMON_400_002);
		}

		// 2) 토글: 이미 즐겨찾기면 해제(DELETE), 아니면 추가(INSERT).
		boolean exists = menuFavoriteMapper.countFavorite(
				param.cmpnyCd(), param.userCd(), param.menuDId()) > 0;

		boolean favoriteAfter;
		if (exists) {
			menuFavoriteMapper.deleteFavorite(param.cmpnyCd(), param.userCd(), param.menuDId());
			favoriteAfter = false;
			log.info("즐겨찾기 해제 - cmpnyCd={}, userCd={}, menuDId={}",
					param.cmpnyCd(), param.userCd(), param.menuDId());
		} else {
			menuFavoriteMapper.insertFavorite(param.cmpnyCd(), param.userCd(), param.menuDId());
			favoriteAfter = true;
			log.info("즐겨찾기 추가 - cmpnyCd={}, userCd={}, menuDId={}",
					param.cmpnyCd(), param.userCd(), param.menuDId());
		}

		return MenuFavoriteToggleResponse.builder()
				.menuDId(param.menuDId())
				.favorite(favoriteAfter)
				.build();
	}
}
