package com.prafta.web.user.user02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthMenuInfoRequest{
	private String authCd;
	private String menuDId;
	private String useYn;
	private String btnSrch;
	private String btnNew;
	private String btnDel;
	private String btnSave;
	private String btnExcl;
}
