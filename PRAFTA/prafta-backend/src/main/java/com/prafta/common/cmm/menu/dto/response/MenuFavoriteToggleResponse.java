package com.prafta.common.cmm.menu.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * 즐겨찾기 토글 결과 응답. 토글 후 해당 메뉴의 즐겨찾기 상태를 돌려준다.
 */
@Getter
@Builder
public class MenuFavoriteToggleResponse {
	/** 토글 대상 MENU_D_ID. */
	private String menuDId;
	/** 토글 후 즐겨찾기 상태(true=추가됨, false=해제됨). 프론트 고정 계약 키 "isFavorite". */
	@JsonProperty("isFavorite")
	private boolean favorite;
}
