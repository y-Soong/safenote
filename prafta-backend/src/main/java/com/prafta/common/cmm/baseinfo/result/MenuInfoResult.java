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
}
