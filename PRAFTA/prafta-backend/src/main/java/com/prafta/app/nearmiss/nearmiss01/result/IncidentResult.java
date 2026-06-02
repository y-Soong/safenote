package com.prafta.app.nearmiss.nearmiss01.result;

/**
 * 아차사고/사건 단건 결과 VO (tb_near_miss 컬럼 기준, camelCase 매핑).
 *
 * <p>웹(com.prafta.web.nearmiss.nearmiss01) IncidentResult 를 앱에 미러링한 사본이다.
 *    앱은 web mapper/service 에 의존하지 않고 동일 테이블만 공유한다(app-010 완전분리 원칙).
 *    웹 사본과의 차이: 관리자 임시조치 메모(adminTempActionDesc) 필드를 추가로 노출한다
 *    (prafta-app-012 D-A1: tb_near_miss.ADMIN_TEMP_ACTION_DESC).
 *
 * <p>코드값(*Cd)과 함께 코드명(*Nm), 파일경로(filePath)를 join/함수로 해석해 담는다.
 */
public record IncidentResult(
    String cmpnyCd
    , String siteCd
    , String nearMissId
    , String incidentTypeCd
    , String incidentTypeNm
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
