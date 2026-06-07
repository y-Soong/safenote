package com.prafta.web.tbm.tbm02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 교육종료(COMPLETED) 전이 응답(prafta-051-05).
 *
 * <p>전이 직후 발급된 종료비번과 전이 상태를 반환한다. 입실비번은 변경되지 않으므로
 * 응답에 없다(상세 조회로 노출).
 */
@Getter
@Builder
public class SessionCompleteResponse {
	private String sessionCd;
	private String statusCd;		// 'COMPLETED'
	private String exitPwd;			// 종료비번(발급값)
}
