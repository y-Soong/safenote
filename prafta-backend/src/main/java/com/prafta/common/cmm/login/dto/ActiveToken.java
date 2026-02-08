package com.prafta.common.cmm.login.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActiveToken {
	String cmpnyCd;
	String userId;
	String tokenId;
	String clientType;
	String refreshTokenHash;
	String expireDtime;
}
