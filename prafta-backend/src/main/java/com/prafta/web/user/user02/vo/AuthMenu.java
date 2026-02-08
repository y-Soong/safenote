package com.prafta.web.user.user02.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthMenu{
	String chk;
	String menuMId;
	String menuMNm;
	String menuSrc;
	String menuDId;
	String menuDNm;
	String useYn;
	String btnSrch;
	String btnNew;
	String btnDel;
	String btnSave;
	String btnExcl;
}
