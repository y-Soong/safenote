package com.prafta.web.user.user06.application.query;

/**
 * 블랙리스트 목록 조회 쿼리.
 *
 * <p>회사 스코프(gvCmpnyCd) 강제. 전화 검색은 평문 대신 mblNoHmac(정확매칭)/mblNoLast4(부분매칭) 파생값만 전달.
 * useYn 은 'Y'/'N'/null(전체). limit/offset 은 서버에서 산출(페이징 상한).
 */
public record BlacklistListQuery(
    String gvCmpnyCd
    , String mblNoHmac
    , String mblNoLast4
    , String useYn
    , int limit
    , int offset
){
}
