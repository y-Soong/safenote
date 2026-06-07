package com.prafta.app.tbm.admin.application.command;

/**
 * R3 T1/T2 상태 전이 커맨드(교육 시작/종료 + 종료 자동이수 일괄).
 *
 * <p>대상 식별자(sessionCd)와 감사 컬럼(gvCmpnyCd/gvUserCd)만 보유한다. 전이 후 상태/시각/가드는
 * 각 SQL 에서 고정한다(start: IN_PROGRESS·STARTED_AT, end: COMPLETED·ENDED_AT, autoComplete: 출결 일괄).
 */
public record AdminSessionTransitionCommand(
    String sessionCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AdminSessionTransitionCommand of(String sessionCd, String gvCmpnyCd, String gvUserCd) {
        return new AdminSessionTransitionCommand(sessionCd, gvCmpnyCd, gvUserCd);
    }
}
