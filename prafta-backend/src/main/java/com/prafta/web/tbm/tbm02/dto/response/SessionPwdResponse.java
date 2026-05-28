package com.prafta.web.tbm.tbm02.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 비밀번호 재발급 응답. */
@Getter
@Builder
public class SessionPwdResponse {
	private String sessionCd;
	private String entryPwd;
	private String exitPwd;
}
