package com.prafta.common.cmm.dailyjoin.result;

/**
 * 일일사용자 회원가입 - 필수약관 조회 결과.
 * TB_TERMS REQUIRED_YN='Y' AND USE_YN='Y' 기준.
 */
public record RequiredTermsResult(
    String termsId
    , String termsVersion
) {
}
