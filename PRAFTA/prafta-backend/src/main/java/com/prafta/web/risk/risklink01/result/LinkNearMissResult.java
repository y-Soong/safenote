package com.prafta.web.risk.risklink01.result;

/**
 * 연결 후보/연결됨 아차사고 목록 결과 VO (tb_near_miss 기준, camelCase 매핑).
 * 상세 열람 재사용(GET /webApi/nearmiss01/incident-info)을 위해 siteCd/nearMissId 키도 보유.
 */
public record LinkNearMissResult(
    String siteCd
    , String nearMissId
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String potentialSeverityNm
    , String reportStatusCd
    , String reportStatusNm
){
}
