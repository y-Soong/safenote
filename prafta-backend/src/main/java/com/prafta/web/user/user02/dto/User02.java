package com.prafta.web.user.user02.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User02 extends cmmReqDto{
	private String chk;
	private String authCd;
	private String menuMId;
	private String menuMNm;
	private String menuDId;
	private String menuDNm;
	private String useYn;
	private String btnSrch;
	private String btnNew;
	private String btnDelt;
	private String btnSave;
	private String btnExcl;
}
