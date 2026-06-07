package com.prafta.web.acct.acct01.result;

/**
 * 위험성평가 연계 조회 결과 VO (3계층 매칭, 사고일 -3M ~ 사고일 유효 평가).
 */
public record RiskLinkResult(
    String processCd
    , String processNm
    , String riskTypeCd
    , String riskTypeNm
    , String hazardCd
    , String hazardNm
    , String assessmentCd
    , String assessmentStatus
    , String assessmentStatusNm
    , String initRiskLv
    , String revalRiskLv
    , String initAssessDate
){
}
