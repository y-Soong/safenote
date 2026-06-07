package com.prafta.app.tbm.admin.result;

/** 세션 상세 - 연계 위험성평가 매핑(표시명은 서비스에서 합성). */
public record AdminSessionRiskResult(
    String siteCd
    , String processCd
    , String processNm
    , String riskTypeCd
    , String riskTypeNm
    , String hazardCd
    , String hazardNm
    , String assessmentCd
    , String assessmentStatus
    , String assessmentStatusNm
    , int displayOrder
){
}
