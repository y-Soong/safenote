package com.prafta.web.tbm.tbmai01.application.model;

/**
 * 비동기 VLM 분석 큐잉 대상 항목(selectAnalyzeTargets 결과 / reanalyze 단건 구성).
 *
 * <p>★MyBatis 위치매핑: 필드 순서는 {@code TbmAi01Mapper.selectAnalyzeTargets} 의 SELECT 컬럼(AS 별칭)
 *    순서와 일치시킨다.
 */
public record TbmAiAnalyzeTarget(
    String mtrlItemCd
    , String mtrlItemType
    , String fileMgmtCd
) {}
