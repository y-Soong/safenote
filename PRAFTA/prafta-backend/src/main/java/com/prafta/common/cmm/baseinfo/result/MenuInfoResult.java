package com.prafta.common.cmm.baseinfo.result;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MenuInfoResult {
	String keyId;
	String id;
	String topMenuNm;
	String sideMenu;
	String route;
	String btnSrch;
	String btnNew;
	String btnDelt;
	String btnSave;
	String btnExcl;
	/** TOP 메뉴(TB_SYST_MENU_M) 정렬 순번 */
	Integer menuIdx;
	/** SIDE 메뉴(TB_SYST_MENU_D) 정렬 순번 — 중분류 그룹 내 순서 */
	Integer menuSubIdx;
	/** 중분류명(TB_SYST_MENU_D.SUB_GROUP_NM). NULL/빈값이면 빌더가 대분류명으로 대체(D4). */
	String subGroupNm;
	/** 중분류 순서(TB_SYST_MENU_D.SUB_GROUP_IDX). 탭 내 그룹 정렬. */
	Integer subGroupIdx;
	// 주: 즐겨찾기 여부는 SQL 컬럼이 아니라 빌더(MenuListResBuilder)가 favorites set 으로 SideMenu 에 주입한다.
	//     여기에 필드를 두면 resultType 생성자 위치매핑이 14컬럼↔생성자인자 수 불일치로 깨지므로 두지 않는다.
}
