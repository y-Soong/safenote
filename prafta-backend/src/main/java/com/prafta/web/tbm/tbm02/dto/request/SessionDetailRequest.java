package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-06 세션 상세 조회 요청. */
@Getter
@Setter
@NoArgsConstructor
public class SessionDetailRequest {
	private String sessionCd;
}
