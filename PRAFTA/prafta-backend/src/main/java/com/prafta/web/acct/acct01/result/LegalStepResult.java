package com.prafta.web.acct.acct01.result;

/**
 * ②탭 — 법정절차 + 진행상태 결과 VO (master LEFT JOIN tb_acct_legal_step).
 * D-day/기한 계산은 프론트(deadlineRuleCd + 사고 발생일 기준).
 */
public record LegalStepResult(
    String stepCd
    , String acctGradeCd
    , Integer stepIdx
    , String stepNm
    , String actionGuide
    , String legalBasis
    , String deadlineRuleCd
    , String stepNote
    , String isDoneYn      // 미저장 시 'N'
    , String doneDtime
    , String doneUserCd
    , String doneUserNm
    , String remark
){
}
