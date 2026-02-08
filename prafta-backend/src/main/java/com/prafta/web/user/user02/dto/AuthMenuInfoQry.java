package com.prafta.web.user.user02.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthMenuInfoQry{
	String authCd;
	String menuDId;
	String useYn;
	String btnSrch;
	String btnNew;
	String btnDel;
	String btnSave;
	String btnExcl;
}
