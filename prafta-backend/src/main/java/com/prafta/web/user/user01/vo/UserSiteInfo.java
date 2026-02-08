package com.prafta.web.user.user01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSiteInfo{
	String cmpnyCd;
	String userId;
	String siteCd;
	String useYn;
}
