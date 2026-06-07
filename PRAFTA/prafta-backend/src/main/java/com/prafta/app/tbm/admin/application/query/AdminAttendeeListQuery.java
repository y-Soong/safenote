package com.prafta.app.tbm.admin.application.query;

/**
 * R3 출결 리스트 조회 Query. 식별자(회사)는 토큰 출처.
 *
 * <p>liveOnly=true 면 입실자만(ENTRY_AT IS NOT NULL, 진행화면 LIVE), false 면 출결 전체(종료화면 COMPLETED).
 */
public record AdminAttendeeListQuery(
    String sessionCd
    , String gvCmpnyCd
    , boolean liveOnly
){
    public static AdminAttendeeListQuery of(String sessionCd, String gvCmpnyCd, boolean liveOnly) {
        return new AdminAttendeeListQuery(sessionCd, gvCmpnyCd, liveOnly);
    }
}
