package com.prafta.web.user.user02.dto;

import lombok.Data;

@Data
public class AuthMenuInfoReq{
	String authCd;
	String menuDId;
	String useYn;
	String btnSrch;
	String btnNew;
	String btnDel;
	String btnSave;
	String btnExcl;
}
