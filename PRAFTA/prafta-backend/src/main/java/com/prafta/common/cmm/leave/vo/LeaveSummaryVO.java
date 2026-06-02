package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 직원별 연차 상세(attd09) 통계 카드용 요약(부여/사용/잔여 + 임박 만료일).
 *
 * <p>{@code expiresAt}는 가장 임박한 ACTIVE 법정 만료일(YYYYMMDD, 없으면 null). 법정외 요약은 null.
 */
@Getter
@Builder
public class LeaveSummaryVO {

    /** 부여 일수 합계 */
    private final BigDecimal granted;

    /** 사용 일수 합계 */
    private final BigDecimal used;

    /** 잔여 일수 (granted - used) */
    private final BigDecimal remaining;

    /** 가장 임박한 ACTIVE 만료일 (YYYYMMDD, 없으면 null) */
    private final String expiresAt;
}
