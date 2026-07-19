package com.prafta.web.subcon.subcon02.result;

/**
 * 사업장 연동 링크 원시행(내부 전용 — 전이 성공 직후 미러 생성/독립화 처리용).
 *
 * <p>조건부 UPDATE 성공(또는 관계 해지 훅)으로 당사자성이 기증명된 경로에서만 사용한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SiteLinkRaw(
    Long linkId
    , Long relationId
    , String srcCmpnyCd
    , String srcSiteCd
    , String dstCmpnyCd
    , String dstSiteCd
    , String status
){
}
