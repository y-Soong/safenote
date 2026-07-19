package com.prafta.web.subcon.subcon02.result;

/**
 * 제공측(원본) 사업장 원시행 — 수락 트랜잭션의 SITE_NO 보정(D4)·루트노드 명명용.
 *
 * <p>cross-tenant read 는 링크 당사자 검증(조건부 UPDATE 선점) 후에만 수행한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SiteSrcRaw(
    String siteNo
    , String siteNm
){
}
