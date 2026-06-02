package com.prafta.common.cmm.dailyjoin.result;

/**
 * 일일사용자 회원가입 - 회사/사업장 정보 조회 결과.
 */
public record SiteInfoResult(
    String cmpnyCd
    , String cmpnyNm
    , String siteCd
    , String siteNm
) {
}
