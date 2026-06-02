package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-06 비밀번호 재발급 요청(OPENED 상태만 허용). */
@Getter
@Setter
@NoArgsConstructor
public class SessionPwdRequest {
	private String sessionCd;
}
