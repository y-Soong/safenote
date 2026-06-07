package com.prafta.web.acct.acct01.result;

/**
 * 순회점검 연계 조회 결과 VO (점검대상 CHKPT_CD 별 1주일 집계).
 * 양호(SYS009 Y) / 불량(N) 카운트와 불량항목 비고 목록(별도 조회).
 */
public record PatrolLinkResult(
    String chkptCd
    , String chkptNm
    , String chklstType
    , Integer totalCnt   // 총 점검항목 수(answer 행 수)
    , Integer goodCnt    // 양호(INSPECT_ANSWER_TYPE='Y')
    , Integer badCnt     // 불량(INSPECT_ANSWER_TYPE='N')
){
}
