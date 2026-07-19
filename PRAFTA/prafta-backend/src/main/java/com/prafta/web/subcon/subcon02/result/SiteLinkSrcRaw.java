package com.prafta.web.subcon.subcon02.result;

/**
 * 사업장의 연동 출처(LINK_SRC_*) 원시행 — 루프 가드 조상 순회용(PRAFTA-SUBCON-T2 §5-3 #4).
 *
 * <p>linkSrcCmpnyCd 가 NULL 이면 원본(체인 루트). 순회는 서버 데이터(tb_site)만 사용한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SiteLinkSrcRaw(
    String linkSrcCmpnyCd
    , String linkSrcSiteCd
){
}
