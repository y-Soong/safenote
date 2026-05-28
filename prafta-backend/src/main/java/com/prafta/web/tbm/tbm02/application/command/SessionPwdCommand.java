package com.prafta.web.tbm.tbm02.application.command;

/** TB_TBM_SESSION 입실/종료 비밀번호 재발급 UPDATE 커맨드. */
public record SessionPwdCommand(
	String sessionCd
	, String entryPwd
	, String exitPwd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static SessionPwdCommand of(
			String sessionCd, String entryPwd, String exitPwd,
			String gvCmpnyCd, String gvUserCd) {

		return new SessionPwdCommand(sessionCd, entryPwd, exitPwd, gvCmpnyCd, gvUserCd);
	}
}
