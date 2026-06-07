package com.prafta.web.acct.acct01.result;

/**
 * ③탭 — 처리 이력 파생 롤업 결과 VO.
 * ②탭의 완료(IS_DONE_YN='Y') 절차를 완료일시순으로 정렬한 읽기전용 뷰.
 */
public record LegalStepHistoryResult(
    String stepCd
    , String stepNm
    , String doneDtime
    , String doneUserCd
    , String doneUserNm
    , String remark
){
}
