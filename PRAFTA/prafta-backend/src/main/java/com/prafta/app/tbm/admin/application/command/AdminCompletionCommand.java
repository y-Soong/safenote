package com.prafta.app.tbm.admin.application.command;

/**
 * R3 T4 개별 이수처리 UPDATE 커맨드.
 *
 * <p>completionStatusCd ∈ {COMPLETED, NOT_COMPLETED}. notCompletedReason 은 NOT_COMPLETED 시 입력값,
 * COMPLETED 시 NULL(서비스에서 결정). STATUS_UPDATED_BY=gvUserCd.
 */
public record AdminCompletionCommand(
    String sessionCd
    , String attendanceCd
    , String completionStatusCd
    , String notCompletedReason
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminCompletionCommand of(String sessionCd, String attendanceCd,
            String completionStatusCd, String notCompletedReason, String gvCmpnyCd, String gvUserCd) {

        return new AdminCompletionCommand(sessionCd, attendanceCd, completionStatusCd,
                notCompletedReason, gvCmpnyCd, gvUserCd);
    }
}
