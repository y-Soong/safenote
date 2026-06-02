package com.prafta.common.cmm.login.application.query;

import java.util.Objects;

import com.prafta.common.cmm.login.application.param.LoginParam;

public record LoginQuery(
		String userId	
	) {
	
	public static LoginQuery from(LoginParam param) {
		Objects.requireNonNull(param, "param is required");
		
		return new LoginQuery(
				param.userId()
	        );
	}
}
