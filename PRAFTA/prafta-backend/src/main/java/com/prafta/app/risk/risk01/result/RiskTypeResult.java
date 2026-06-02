package com.prafta.app.risk.risk01.result;

/**
 * prafta-036-B2: 위험성평가 분류 조회 결과.
 *
 * <p>매핑 대상: AppRisk01Mapper.selectRiskType SELECT 컬럼 (underscore-to-camelCase).
 * <pre>
 *   1. PROCESS_CD     -> processCd     (⚠ 기존 vo 의 'ProcessCd' 오타 정정)
 *   2. RISK_TYPE_CD   -> riskTypeCd
 *   3. RISK_TYPE_NM   -> riskTypeNm
 * </pre>
 *
 * <p>FE 호환 확인 결과(Risk_01.vue L530, L656, L682):
 *   `item.processCd === processCd.value` 로 destructure -> FE 는 이미 소문자 `processCd` 를 기대.
 *   기존 vo 가 대문자 `ProcessCd` 로 직렬화하던 시점에는 FE 필터링이 깨져있었으며,
 *   본 정정으로 자연스럽게 회복된다(B-1 qa 잔존 위험 D-R1 동시 해소).
 */
public record RiskTypeResult(
    String processCd
    , String riskTypeCd
    , String riskTypeNm
) {

}
