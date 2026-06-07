package com.prafta.web.tbm.tbm02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 교육준비(OPENED) 전이 응답(prafta-051-03).
 *
 * <p>전이 직후 발급된 입실비번과 전이 상태를 반환한다. 종료비번은 이 시점에 발급하지
 * 않으므로 응답에 없다. 위험성평가 0건 경고는 warningMessage 로 함께 전달한다.
 */
@Getter
@Builder
public class SessionPrepareResponse {
	private String sessionCd;
	private String statusCd;		// 'OPENED'
	private String entryPwd;		// 입실비번(발급값)
	private String warningMessage;	// 위험성평가 0건일 때만
}
