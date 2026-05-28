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
	/** SIDE 메뉴(TB_SYST_MENU_D) 정렬 순번 */
	Integer menuSubIdx;
}
