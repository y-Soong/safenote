package com.prafta.app.tbm.admin.result;

/** 위험성평가 선택 모달 옵션(표시명은 서비스에서 합성). */
public record AdminRiskOptionResult(
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
){
}
