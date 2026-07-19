package com.prafta.web.subcon.subcon02.result;

/**
 * 점검 결과 write-through 체인의 점검대상(티어) 좌표(PRAFTA-SUBCON-T6-05 / qa M-3).
 *
 * <p>과거에는 (점검대상 + 문항) 좌표를 문항마다 재해석했다(N+1). 점검대상 매핑은 전 문항에 대해 불변이므로,
 * 저장 1회당 점검대상 체인만 1회 해석하고 문항 좌표는 사이트 쌍 매핑표로 치환한다.
 *
 * <p>record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ChkptTierRaw(
    String cmpnyCd
    , String siteCd
    , String chkLstType
    , String chkptCd
){
}
