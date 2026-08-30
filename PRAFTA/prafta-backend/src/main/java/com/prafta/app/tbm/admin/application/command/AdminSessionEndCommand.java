package com.prafta.app.tbm.admin.application.command;

/**
 * tbm04-manager-sign: 교육 종료(IN_PROGRESS→COMPLETED) UPDATE 커맨드.
 *
 * <p>종료비번(EXIT_PWD) 최초 발급(prafta-051 E5) + 주관자 서명 파일코드/서명시각(MANAGER_SIGN_*)을
 * 한 UPDATE 로 반영한다. {@code AdminSessionSinglePwdCommand} 는 updateEntryPwd/updateExitPwd 공용이라
 * 확장하지 않고 endSession 전용 커맨드를 신설했다(계약 격리).
 */
public record AdminSessionEndCommand(
    String sessionCd
    , String exitPwd
    , String managerSignFileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionEndCommand of(String sessionCd, String exitPwd,
            String managerSignFileMgmtCd, String gvCmpnyCd, String gvUserCd) {

        return new AdminSessionEndCommand(sessionCd, exitPwd, managerSignFileMgmtCd, gvCmpnyCd, gvUserCd);
    }
}
