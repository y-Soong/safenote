package com.prafta.common.cmm.auth.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserInfo {
	String cmpnyCd;
	String userId;
	String userNm;
	String siteCd;
	String siteNo;
	String siteNm;
	String authCd;
	String mblNo;
	String email;
}
