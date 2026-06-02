package com.prafta.web.user.user02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user02.application.command.AuthMenuInfoCommand;
import com.prafta.web.user.user02.application.query.AuthMenuListQuery;
import com.prafta.web.user.user02.result.AuthMenuResult;
import com.prafta.web.user.user02.result.MenuDMenuMResult;

@Mapper
public interface User02Mapper {
	List<AuthMenuResult> selectAuthMenuList(AuthMenuListQuery query);

	void mergeAuthMenuInfo(AuthMenuInfoCommand command);

	// PRAFTA-042-1: 잠금 판정용 menuDId -> menuMId 매핑 일괄 조회(N+1 회피, 일괄 IN 조회).
	// tb_syst_menu_d 의 PK 가 (MENU_D_ID, MENU_M_ID) 라 한 menuDId 가 복수 menuMId 를 가질 수 있어 List 로 반환.
	List<MenuDMenuMResult> selectMenuMIdsByMenuDIds(@Param("menuDIds") List<String> menuDIds);
}
