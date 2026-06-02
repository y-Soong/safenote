package com.prafta.app.risk.risk01.result;

/**
 * prafta-036-B2: 위험성평가 발생상황(유해요인) 조회 결과.
 *
 * <p>매핑 대상: AppRisk01Mapper.selectRiskHazard SELECT 컬럼 (underscore-to-camelCase).
 * <pre>
 *   1. RISK_TYPE_CD  -> riskTypeCd
 *   2. HAZARD_CD     -> hazardCd
 *   3. HAZARD_NM     -> hazardNm
 * </pre>
 */
public record RiskHazardResult(
    String riskTypeCd
    , String hazardCd
    , String hazardNm
) {

}
