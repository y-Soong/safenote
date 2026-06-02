package com.prafta.web.user.user02.result;

/**
 * PRAFTA-042-1: 화면(MENU_D_ID) → 대메뉴(MENU_M_ID) 매핑 조회 결과.
 * 잠금 판정({@link com.prafta.common.util.MenuLockPolicy})에 사용된다.
 */
public record MenuDMenuMResult(
	String menuDId
	, String menuMId
){
}
