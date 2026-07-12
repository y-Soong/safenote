package com.prafta.common.cmm.login.application.command;

import java.util.Objects;

public record AuthMenuInfoCommand(
		String cmpnyCd
		, String systValDCd
        , String userId
) {
    public static AuthMenuInfoCommand from(String cmpnyCd, String systValDCd, String userId) {
    	Objects.requireNonNull(cmpnyCd, "cmpnyCd is required");
    	Objects.requireNonNull(systValDCd, "systValDCd is required");
    	Objects.requireNonNull(userId, "userId is required");

    	// cmpnyCd 는 호출 측(=JWT)에서 전달된 서버 신뢰값. 회사 스코프 약관 동의 적재용.
    	return new AuthMenuInfoCommand(
			cmpnyCd
			, systValDCd
			, userId
		);
    }
}