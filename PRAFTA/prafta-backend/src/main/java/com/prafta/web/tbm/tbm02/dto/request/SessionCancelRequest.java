package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-06 세션 취소 요청(DRAFT/OPENED 상태만 허용). */
@Getter
@Setter
@NoArgsConstructor
public class SessionCancelRequest {
	private String sessionCd;
	private String cancelReason;	// 필수
}
