package com.prafta.web.nearmiss.nearmiss01.result;

/**
 * 아차사고/사건 단건 결과 VO (tb_near_miss 컬럼 기준, camelCase 매핑).
 * 코드값(*Cd)과 함께 코드명(*Nm), 파일경로(filePath)를 join/함수로 해석해 담는다.
 *
 * <p>⚠️ MyBatis record 위치매핑 — {@code NearMiss01Mapper.xml} 의 {@code incidentColumns}
 *    SELECT 컬럼 순서와 아래 필드 순서가 반드시 일치해야 한다(앱 사본 IncidentResult 와 동일 배치).
 */
public record IncidentResult(
    String cmpnyCd
    , String siteCd
    , String nearMissId
    , String processCd
    , String processNm
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String potentialSeverityNm
    , String immediateActionDesc
    , String adminTempActionDesc
    , String causeDesc
    , String preventionDesc
    , String fileMgmtCd
    , String fileName
    , String filePath
    , String reportStatusCd
    , String reportStatusNm
    , String reporterId
    , String reporterNm
    , String reportDtime
    , String reviewerId
    , String reviewerNm
    , String reviewDtime
    , String rejectReason
    , String useYn
){
}
