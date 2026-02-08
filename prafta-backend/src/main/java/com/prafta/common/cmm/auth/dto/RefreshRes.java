package com.prafta.common.cmm.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefreshRes {
	String token;
}
