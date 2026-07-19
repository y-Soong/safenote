package com.prafta.common.cmm.dailycontract.result;

/**
 * 계약서 서명 게이트 판정 결과 (R2 + D8).
 *
 * <p>signRequiredYn='N' 사유: 활성 계약서 미등록(스킵) / 현재 버전·현재 승인 사이클 서명 완료 /
 * 일용직 아님. 'Y' 이면 contractVer/contractNm 에 서명 대상 활성 계약서 정보가 담긴다.
 */
public record SignGateResult(
    String signRequiredYn      // 'Y' | 'N'
    , Integer contractVer      // 서명 필요 시 대상 버전(스킵이면 null)
    , String contractNm        // 서명 필요 시 계약서명(스킵이면 null)
) {
    public static SignGateResult skip() {
        return new SignGateResult("N", null, null);
    }

    public static SignGateResult required(int contractVer, String contractNm) {
        return new SignGateResult("Y", contractVer, contractNm);
    }
}
