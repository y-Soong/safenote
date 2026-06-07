package com.prafta.app.tbm.admin.application.command;

/**
 * TB_TBM_SESSION_STATE 초기 UPSERT 커맨드.
 *
 * <p>개설(OPENED) 시 초기 row 생성(CURRENT_SLIDE_INDEX=0, SYNC_STATE_CD='PAUSED').
 * 실제 동기화 쓰기 경로는 후속(라이브) 라운드 소관이며 본 라운드는 초기값만 등록한다.
 */
public record AdminSessionStateCommand(
    String sessionCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionStateCommand of(String sessionCd, String gvCmpnyCd, String gvUserCd) {
        return new AdminSessionStateCommand(sessionCd, gvCmpnyCd, gvUserCd);
    }
}
