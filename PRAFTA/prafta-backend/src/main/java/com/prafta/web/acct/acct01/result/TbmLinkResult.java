package com.prafta.web.acct.acct01.result;

/**
 * TBM 연계 조회 결과 VO (사고 발생 당일 세션 + 재해자 이수여부).
 * 재해자 출결 기록이 없으면 victimCompletionStatusCd 는 null("시스템 기록 없음").
 */
public record TbmLinkResult(
    String sessionCd
    , String siteCd
    , String title
    , String statusCd
    , String statusNm
    , String managerUserCd
    , String managerUserNm
    , String openedAt
    , String victimCompletionStatusCd
    , String victimCompletionStatusNm
){
}
