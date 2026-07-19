package com.prafta.web.subcon.subcon02.result;

/**
 * 연동 제안 후보 — 내 활성 사업장 1건(PRAFTA-SUBCON-T2 §5-2).
 *
 * <p>미러 포함(미러 재제안 = n차 체인의 본체). linkYn='Y' 면 받은 미러 사업장.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record MySiteResult(
    String siteCd
    , String siteNm
    , String linkYn
){
}
