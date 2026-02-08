package com.prafta.web.user.user02.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthMenuListQry{
	String menuDNm;
	String menuMNm;
	String authCd;
	String useYn;
}
