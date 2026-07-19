package com.prafta.common.cmm.dailycontract.result;

/**
 * 사업장 활성(USE_YN='Y') 계약서 단건 (TB_DAILY_CONTRACT — 기능성 유니크로 최대 1건).
 *
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record ActiveContractResult(
    String siteCd
    , int contractVer
    , String contractNm
    , String fileMgmtCd
    , String insertDate    // YYYY-MM-DD HH:mm (표시용)
) {
}
