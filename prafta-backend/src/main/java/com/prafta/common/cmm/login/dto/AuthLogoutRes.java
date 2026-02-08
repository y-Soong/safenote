package com.prafta.common.cmm.login.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthLogoutRes {
	private String message;
    private int revokedCount;
}
