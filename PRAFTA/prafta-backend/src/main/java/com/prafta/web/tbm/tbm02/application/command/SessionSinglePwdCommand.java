package com.prafta.web.tbm.tbm02.application.command;

/**
 * 입실/종료 비밀번호 단일 컬럼 UPDATE 커맨드(prafta-051-02).
 *
 * <p>입실비번과 종료비번을 분리 발급/재발급하기 위해 한 번에 한 컬럼만 갱신한다(두 비번을
 * 동시에 덮어쓰면 종료비번까지 새로 덮는 부작용이 있어, 입실/종료 전용 매퍼와 함께 사용한다).
 */
public record SessionSinglePwdCommand(
	String sessionCd
	, String pwd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionSinglePwdCommand of(
			String sessionCd, String pwd, String gvCmpnyCd, String gvUserCd) {

		return new SessionSinglePwdCommand(sessionCd, pwd, gvCmpnyCd, gvUserCd);
	}
}
