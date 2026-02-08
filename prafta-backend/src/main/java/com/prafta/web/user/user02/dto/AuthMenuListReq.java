package com.prafta.web.user.user02.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthMenuListReq{
	String menuDNm;
	String menuMNm;
	String authCd;
	String useYn;
}
