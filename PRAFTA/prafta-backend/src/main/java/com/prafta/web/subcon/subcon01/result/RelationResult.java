package com.prafta.web.subcon.subcon01.result;

/**
 * 연동 관계 목록 1행(자사 관점).
 *
 * <p>상대사 노출 필드는 회사코드/회사명만(사업자번호 목록 미노출 — cross-tenant 최소 노출).
 * direction: SENT(자사=요청측) / RECEIVED(자사=상대측) — SQL CASE 산출.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record RelationResult(
    Long relationId
    , String direction
    , String otherCmpnyCd
    , String otherCmpnyNm
    , String status
    , String insertDate
    , String processDtime
    , String processComment
){
}
