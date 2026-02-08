package com.prafta.common.cmm.login.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserLogout {
	String cmpnyCd;
	String userId;
	String clientType;
	String deviceId;
}
