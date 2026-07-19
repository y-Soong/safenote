package com.prafta.web.subcon.subcon02.result;

/**
 * 연동 제안 후보 — 관계 ACCEPTED 상대 회사 1건(회사코드/회사명만 — 최소 노출, PRAFTA-SUBCON-T2 §5-2).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record RelationCmpnyResult(
    String cmpnyCd
    , String cmpnyNm
){
}
