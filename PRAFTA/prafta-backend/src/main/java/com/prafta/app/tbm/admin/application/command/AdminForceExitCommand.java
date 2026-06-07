package com.prafta.app.tbm.admin.application.command;

/**
 * R3 T3 강제 퇴실 UPDATE 커맨드.
 *
 * <p>EXIT_TYPE_CD='MANAGER_FORCED', COMPLETION_STATUS_CD='NOT_COMPLETED' 는 SQL 에서 고정한다.
 * exitForcedReason 은 nullable(빈문자는 서비스에서 NULL 로 정규화). EXIT_BY_MANAGER_USER_CD=gvUserCd.
 */
public record AdminForceExitCommand(
    String sessionCd
    , String attendanceCd
    , String exitForcedReason
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminForceExitCommand of(String sessionCd, String attendanceCd,
            String exitForcedReason, String gvCmpnyCd, String gvUserCd) {

        return new AdminForceExitCommand(sessionCd, attendanceCd, exitForcedReason, gvCmpnyCd, gvUserCd);
    }
}
