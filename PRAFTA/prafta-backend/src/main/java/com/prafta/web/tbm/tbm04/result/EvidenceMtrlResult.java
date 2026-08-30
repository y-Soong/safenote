package com.prafta.web.tbm.tbm04.result;

/**
 * TBM 증빙 교육일지(건별) 교육자료 1행.
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record EvidenceMtrlResult(
    String sessionCd
    , String mtrlTitle
){
}
