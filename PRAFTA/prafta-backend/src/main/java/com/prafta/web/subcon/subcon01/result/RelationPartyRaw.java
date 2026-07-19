package com.prafta.web.subcon.subcon01.result;

/**
 * 관계 당사자/상태 원시 1행(내부 판정용 — 프론트 미노출).
 *
 * <p>이력·해지요약의 당사자 검증, 해지 훅의 양측 회사코드 전달에 사용한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record RelationPartyRaw(
    String reqCmpnyCd
    , String tgtCmpnyCd
    , String status
){
}
