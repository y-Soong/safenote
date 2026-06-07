package com.prafta.web.acct.acct01.result;

/**
 * 사고 단건/목록 결과 VO (tb_acct 컬럼 기준, camelCase 매핑).
 * 재해자 PII 는 마스킹 이름(victimUserNm) + 휴대폰 끝4자리(victimMblNoLast4)만 담는다.
 */
public record AcctResult(
    String cmpnyCd
    , String siteCd
    , String siteNm
    , String acctId
    , String victimUserTypeCd
    , String victimUserTypeNm
    , String victimUserCd
    , String victimUserNm
    , String victimMblNoLast4
    , String occurYmd
    , String occurTime
    , String occurPlace
    , String acctGradeCd
    , String acctGradeNm
    , String acctDesc
    , String employerDesc
    , String processStatusCd
    , String processStatusNm
    , String insertNo
    , String insertDate
    , String updateNo
    , String updateDate
){
}
