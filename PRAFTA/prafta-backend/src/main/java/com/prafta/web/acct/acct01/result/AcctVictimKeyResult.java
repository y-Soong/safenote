package com.prafta.web.acct.acct01.result;

/**
 * 사고 재해자 식별키 VO (대표 승계용 최소 순번 인원 조회).
 */
public record AcctVictimKeyResult(
    Integer victimSeq
    , String userTypeCd
    , String userCd
){
}
