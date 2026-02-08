package com.prafta.web.user.user01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserInfoListQry{
	String userId;
	String userNm;
	String useYn;
	String siteCd;
}
