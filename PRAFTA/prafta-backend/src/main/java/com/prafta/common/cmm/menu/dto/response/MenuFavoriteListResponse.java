package com.prafta.common.cmm.menu.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 내 즐겨찾기 MENU_D_ID 목록 응답.
 */
@Getter
@Builder
public class MenuFavoriteListResponse {
	/** 즐겨찾기한 MENU_D_ID 목록(등록순). */
	private List<String> favoriteMenuDIds;
}
