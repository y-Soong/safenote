package com.prafta.web.user.user01.result;

/**
 * 경력 인정 항목 조회 결과 (PRAFTA-017-4).
 */
public record ServiceCreditResult(
    String creditId
    , Integer creditMonths
    , String reasonType
    , String reasonDetail
) {
}
