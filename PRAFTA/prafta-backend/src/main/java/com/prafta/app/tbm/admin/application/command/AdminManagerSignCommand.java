package com.prafta.app.tbm.admin.application.command;

/**
 * tbm04-manager-sign: 종료(COMPLETED) 세션 사후 주관자 서명 UPDATE 커맨드.
 *
 * <p>가드(WHERE STATUS_CD='COMPLETED' AND MANAGER_SIGN_FILE_MGMT_CD IS NULL)로 경합을 이중 방어한다.
 */
public record AdminManagerSignCommand(
    String sessionCd
    , String managerSignFileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminManagerSignCommand of(String sessionCd, String managerSignFileMgmtCd,
            String gvCmpnyCd, String gvUserCd) {

        return new AdminManagerSignCommand(sessionCd, managerSignFileMgmtCd, gvCmpnyCd, gvUserCd);
    }
}
