package com.prafta.web.acct.acct01.result;

/**
 * 순회점검 연계 — 불량 항목 상세 결과 VO (양호/불량 집계의 드릴다운).
 */
public record PatrolItemResult(
    String chkptCd
    , String inspectItemCd
    , String inspectItemSubj
    , String workDate
    , String inspectAnswerType // Y/N
    , String answerDesc
){
}
