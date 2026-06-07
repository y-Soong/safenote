package com.prafta.web.acct.acct01.result;

/**
 * 재해자 검색 결과 VO (정규 tb_user + 일용 tb_daily_user UNION).
 * PII 는 마스킹 이름 + 휴대폰 끝4자리만 노출(평문 복호화 금지).
 */
public record VictimResult(
    String userTypeCd   // REGULAR/DAILY
    , String userTypeNm
    , String userCd
    , String userNm     // 마스킹된 이름
    , String mblNoLast4
    , String siteCd
    , String siteNm
    , String nodeNm     // 정규직 부서명(일용직은 null)
){
}
