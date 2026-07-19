package com.prafta.platform.customer.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * AI 토큰 한도 변경 응답 DTO(갱신 후 값 — FE 행 갱신용).
 */
@Value
@Builder
public class TokenQuotaUpdateResponse {

    /** 대상 회사코드. */
    String cmpnyCd;

    /** 갱신된 한도(원시 토큰 수. -1 무제한 / 0 차단 / 양수). */
    long tokenLimit;

    /** 개별 설정 행 존재 여부(UPSERT 직후이므로 항상 'Y'). */
    String quotaCustomYn;
}
