package com.prafta.common.cmm.menu.service;

import com.prafta.common.cmm.menu.application.param.MenuFavoriteListParam;
import com.prafta.common.cmm.menu.application.param.MenuFavoriteToggleParam;
import com.prafta.common.cmm.menu.dto.response.MenuFavoriteListResponse;
import com.prafta.common.cmm.menu.dto.response.MenuFavoriteToggleResponse;

/**
 * LNB 즐겨찾기 서비스.
 */
public interface MenuFavoriteService {

	/** 내 즐겨찾기 MENU_D_ID 목록 조회. */
	MenuFavoriteListResponse selectMyFavorites(MenuFavoriteListParam param);

	/** 즐겨찾기 토글(존재하면 DELETE, 없으면 INSERT). */
	MenuFavoriteToggleResponse toggleFavorite(MenuFavoriteToggleParam param);
}
