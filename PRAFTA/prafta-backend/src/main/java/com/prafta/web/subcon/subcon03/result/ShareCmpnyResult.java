package com.prafta.web.subcon.subcon03.result;

/**
 * 공유 요청 후보 — 관계 ACCEPTED 상대 회사 1건(회사코드/회사명만 — 최소 노출).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ShareCmpnyResult(
    String cmpnyCd
    , String cmpnyNm
){
}
