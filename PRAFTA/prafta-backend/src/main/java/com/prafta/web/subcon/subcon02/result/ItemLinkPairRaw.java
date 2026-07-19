package com.prafta.web.subcon.subcon02.result;

/**
 * 사이트 쌍(수신 ← 원본) 점검문항 매핑 쌍(qa M-3).
 *
 * <p>체인 간선당 1회 조회로 그 간선의 전 문항 좌표를 해석한다. 부모 방향은 (수신 → 원본) 그대로,
 * 자식 방향은 역방향(원본 → 수신)으로 뒤집어 사용한다.
 *
 * <p>record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ItemLinkPairRaw(
    String itemCd
    , String srcItemCd
){
}
