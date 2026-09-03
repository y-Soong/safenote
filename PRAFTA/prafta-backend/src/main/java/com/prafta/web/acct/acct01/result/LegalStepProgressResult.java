package com.prafta.web.acct.acct01.result;

/**
 * 처리상태 파생용 법정단계 진행 집계 결과 VO (PROCESS 단계만, REFERENCE 제외).
 * selectLegalStepProgress 의 SELECT 컬럼 순서 = 생성자 순서 (totalCnt, doneCnt).
 */
public record LegalStepProgressResult(
    Integer totalCnt
    , Integer doneCnt
){
}
