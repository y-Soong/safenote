package com.prafta.app.tbm.admin.application.command;

/** TB_TBM_SESSION 입실/종료 비밀번호 재발급 UPDATE 커맨드. */
public record AdminSessionPwdCommand(
    String sessionCd
    , String entryPwd
    , String exitPwd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionPwdCommand of(
            String sessionCd, String entryPwd, String exitPwd,
            String gvCmpnyCd, String gvUserCd) {

        return new AdminSessionPwdCommand(sessionCd, entryPwd, exitPwd, gvCmpnyCd, gvUserCd);
    }
}
