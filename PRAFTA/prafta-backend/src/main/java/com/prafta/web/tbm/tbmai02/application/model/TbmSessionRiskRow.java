package com.prafta.web.tbm.tbmai02.application.model;

/**
 * 세션 매핑 위험성평가 + 개선항목 flat 조회 행(TbmAi02Mapper.selectSessionRiskRows).
 *
 * <p>개선항목(1:N)은 LEFT JOIN flat 으로 내려오므로 평가 1건이 여러 행으로 반복된다 —
 *    서비스({@code buildRiskLines})에서 (siteCd|processCd|assessmentCd) 키로 그룹핑한다.
 * <p>⚠️ record 위치매핑: 필드 순서 = SELECT 컬럼 순서(프로젝트 관례).
 */
public record TbmSessionRiskRow(
    String siteCd
    , String processCd
    , String assessmentCd
    , String assessmentDesc
    , String initDesc
    , String initRiskLv
    , String revalBeforeDesc
    , String revalDesc
    , String improveDesc
) {}
