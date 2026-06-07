package com.prafta.web.tbm.tbm02.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 종료 비밀번호 재발급 응답(prafta-051-02, COMPLETED 상태만). */
@Getter
@Builder
public class SessionExitPwdResponse {
	private String sessionCd;
	private String exitPwd;
}
