package com.prafta.web.user.user03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSiteAuthRequest {
	private String chk;
	private String cmpnyCd;
	private String userCd;
	private String siteCd;
	private String allocYn;
	private String useYn;

}
