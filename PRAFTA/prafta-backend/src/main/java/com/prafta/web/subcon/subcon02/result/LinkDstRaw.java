package com.prafta.web.subcon.subcon02.result;

/**
 * 전파 대상(직속 미러) 원시행 — SiteLinkPropagationService 재귀용(PRAFTA-SUBCON-T2 T2-05).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record LinkDstRaw(
    Long linkId
    , String dstCmpnyCd
    , String dstSiteCd
){
}
