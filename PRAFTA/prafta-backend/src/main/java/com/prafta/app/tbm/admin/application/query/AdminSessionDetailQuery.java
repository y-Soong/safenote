package com.prafta.app.tbm.admin.application.query;

/** T-A2 세션 상세/게이트/매핑 조회 Query. 식별자(회사)는 토큰 출처. */
public record AdminSessionDetailQuery(
    String sessionCd
    , String gvCmpnyCd
){
    public static AdminSessionDetailQuery of(String sessionCd, String gvCmpnyCd) {
        return new AdminSessionDetailQuery(sessionCd, gvCmpnyCd);
    }
}
