package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연동 회사 지정/해제/조회 요청(PRAFTA-SUBCON-T5).
 *
 * <p>조회(session-shares / session-share-candidates / session-share-allowed-cmpnys)는 sessionCd 만,
 * 지정/해제(session-share-designate / session-share-release)는 shareCmpnyCd 를 함께 보낸다.
 * 회사/권한/행위자 식별자는 모두 JWT 에서 도출한다(클라 입력 불신).
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionShareRequest {
	private String sessionCd;		// 대상 세션
	private String shareCmpnyCd;	// 지정/해제 대상 회사코드(조회 시 미사용)
}
