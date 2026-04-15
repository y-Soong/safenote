package com.prafta.web.user.user02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthMenuListRequest{
	private String menuDNm;
	private String menuMNm;
	private String authCd;
	private String useYn;
}
