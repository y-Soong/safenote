package com.prafta.platform.customer.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 토큰 한도 변경 요청 DTO (POST /platformApi/customer/token-quota).
 *
 * <p>무제한(-1)/차단(0)은 음수 직접 입력 대신 모드로 표현한다(요청서 §4 권장안).
 */
@Getter
@Setter
@NoArgsConstructor
public class TokenQuotaUpdateRequest {

    /** 대상 회사코드(필수). */
    private String cmpnyCd;

    /** 한도 방식: LIMIT(한도 설정) / UNLIMITED(무제한 -1) / BLOCK(완전 차단 0). */
    private String quotaMode;

    /** LIMIT 모드 전용 — 만 단위 정수(1~1,000,000). 저장값 = limitMan * 10,000 토큰. */
    private Integer limitMan;
}
