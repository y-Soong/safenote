package com.prafta.common.cmm.login.application.command;

import java.util.Objects;

public record AuthMenuInfoCommand(
		String systValDCd
        , String userId
) {
    public static AuthMenuInfoCommand from(String systValDCd, String userId) {
    	Objects.requireNonNull(systValDCd, "systValDCd is required");
    	Objects.requireNonNull(userId, "userId is required");
    	
    	return new AuthMenuInfoCommand(
			systValDCd
			, userId
		);
    }
}