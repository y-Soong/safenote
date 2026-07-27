package com.prafta.common.cmm.dailycontract.result;

/**
 * 정정 대상 계약서 잠금 조회 행 (TB_DAILY_CONTRACT — 승인시점 버전확정 T3, {@code FOR UPDATE}).
 *
 * <p>{@code ActiveContractResult} 와 달리 {@code USE_YN} 을 포함한다 — 정정은 "활성 계약서만"
 * 허용하므로(다른 관리자가 그 사이 교체·중지했는지 판정 필요) 잠금 조회에서 상태를 함께 읽는다.
 *
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record ContractLockRow(
    int contractVer
    , String contractNm
    , String fileMgmtCd
    , String useYn        // Y: 활성 / N: 교체·중지됨(정정 불가 → 409_002)
) {
}
