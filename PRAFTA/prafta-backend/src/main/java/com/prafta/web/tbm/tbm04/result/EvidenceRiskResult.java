package com.prafta.web.tbm.tbm04.result;

/**
 * TBM 증빙 교육일지(건별) 위험성평가 연계 1행.
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record EvidenceRiskResult(
    String sessionCd
    , String processNm         // 공정명(COM002)
    , String hazardNm          // 위험요인(자체입력이면 평가 설명)
){
}
