package com.prafta.web.attd.attd09.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

/**
 * 입사일 기준 차액 보전(법정 수기부여) 응답 (경력인정 이원화 Phase 2 §2-3).
 * POST /attd09/leave-grant/cover-grant.
 */
@Value
@Builder
public class CoverGrantResponse {

    String grantId;
    BigDecimal grantedDays;
    BigDecimal remainingShortfallAfter;
}
