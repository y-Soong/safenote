package com.prafta.web.acct.acct01.result;

/**
 * 아차사고 연계 조회 결과 VO (사고일 -3M ~ 사고일, USE_YN='Y').
 */
public record NearMissLinkResult(
    String nearMissId
    , String incidentTypeCd
    , String incidentTypeNm
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String potentialSeverityNm
    , String reportStatusCd
    , String reportStatusNm
){
}
