package com.prafta.app.risk.risk01.result;

/**
 * prafta-036-B2: 위험성평가 구분(공정) 조회 결과.
 *
 * <p>매핑 대상: AppRisk01Mapper.selectRiskCategory SELECT 컬럼 (underscore-to-camelCase).
 * <pre>
 *   1. BAIM_VAL_D_CD  -> baimValDCd
 *   2. BAIM_VAL_D_NM  -> baimValDNm
 *   3. SORT_IDX       -> sortIdx
 * </pre>
 * SORT_IDX 는 DB 상 INT 이나 기존 vo 호환 및 FE 무변경(JSON 키/타입 보존)을 위해
 * String 으로 매핑한다(MyBatis 자동 변환).
 */
public record RiskCategoryResult(
    String baimValDCd
    , String baimValDNm
    , String sortIdx
) {

}
