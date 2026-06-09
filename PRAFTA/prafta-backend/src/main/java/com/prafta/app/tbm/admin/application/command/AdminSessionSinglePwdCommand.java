package com.prafta.app.tbm.admin.application.command;

/**
 * 단일 비밀번호 UPDATE 커맨드(prafta-051 R-A, app 관리자).
 *
 * <p>입실비번 전용 재발급(OPENED, E6)·종료비번 전용 재발급(COMPLETED, E7)·교육종료 시 종료비번 최초
 * 발급(E5)에 공용한다. {@code pwd} 가 매퍼별로 ENTRY_PWD/EXIT_PWD 중 하나로만 반영된다.
 */
public record AdminSessionSinglePwdCommand(
    String sessionCd
    , String pwd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionSinglePwdCommand of(
            String sessionCd, String pwd, String gvCmpnyCd, String gvUserCd) {

        return new AdminSessionSinglePwdCommand(sessionCd, pwd, gvCmpnyCd, gvUserCd);
    }
}
