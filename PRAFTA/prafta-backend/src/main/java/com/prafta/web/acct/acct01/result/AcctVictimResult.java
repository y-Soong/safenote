package com.prafta.web.acct.acct01.result;

/**
 * 사고 재해자 결과 VO (tb_acct_victim + 사용자 표시정보). 위치매핑 — SELECT 컬럼 순서와 1:1.
 * PII 는 마스킹 이름(앞2자 + *) + 휴대폰 끝4자리만(평문 복호화 없음).
 * representativeYn = 헤더(tb_acct.VICTIM_USER_TYPE_CD/VICTIM_USER_CD)와 일치하면 'Y'.
 */
public record AcctVictimResult(
    Integer victimSeq
    , String userTypeCd
    , String userTypeNm
    , String userCd
    , String userNm
    , String mblNoLast4
    , String nodeNm
    , String victimResultCd
    , String victimResultNm
    , Integer careDays
    , Integer restDays
    , String injuryPart
    , String injuryDesc
    , String representativeYn
    , String insertDate
){
}
