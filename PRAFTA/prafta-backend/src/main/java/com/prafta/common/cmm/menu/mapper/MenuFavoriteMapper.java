package com.prafta.common.cmm.menu.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * LNB 즐겨찾기(TB_USER_MENU_FAVORITE) 매퍼.
 *
 * <p>모든 메서드의 CMPNY_CD/USER_CD 는 JWT 도출값만 전달한다(IDOR 방지 — 클라 입력 금지).
 */
@Mapper
public interface MenuFavoriteMapper {

	/** 내 즐겨찾기 MENU_D_ID 목록. */
	List<String> selectFavoriteMenuDIds(
			@Param("cmpnyCd") String cmpnyCd
			, @Param("userCd") String userCd);

	/** 특정 메뉴의 즐겨찾기 존재 여부(토글 분기용). */
	int countFavorite(
			@Param("cmpnyCd") String cmpnyCd
			, @Param("userCd") String userCd
			, @Param("menuDId") String menuDId);

	/** 해당 MENU_D_ID 가 웹 메뉴(menuSrc='001')로 실재하는지 검증(유효성). */
	int countMenuDExists(@Param("menuDId") String menuDId);

	/** 즐겨찾기 추가. */
	int insertFavorite(
			@Param("cmpnyCd") String cmpnyCd
			, @Param("userCd") String userCd
			, @Param("menuDId") String menuDId);

	/** 즐겨찾기 해제(행 삭제). */
	int deleteFavorite(
			@Param("cmpnyCd") String cmpnyCd
			, @Param("userCd") String userCd
			, @Param("menuDId") String menuDId);
}
