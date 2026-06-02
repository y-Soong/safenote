package com.prafta.web.tbm.tbm02.application.command;

/**
 * TB_TBM_SESSION_STATE 초기 UPSERT 커맨드.
 *
 * <p>개설(OPENED) 시 초기 row 생성. 실제 동기화 쓰기 경로는 C 단계 소관이며
 * B는 초기값(CURRENT_SLIDE_INDEX=0, SYNC_STATE_CD='PAUSED')만 등록한다.
 */
public record SessionStateCommand(
	String sessionCd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionStateCommand of(String sessionCd, String gvCmpnyCd, String gvUserCd) {
		return new SessionStateCommand(sessionCd, gvCmpnyCd, gvUserCd);
	}
}
