package com.prafta.common.cmm.login.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthLogoutResponse {
	private String message;
    private int revokedCount;
}
