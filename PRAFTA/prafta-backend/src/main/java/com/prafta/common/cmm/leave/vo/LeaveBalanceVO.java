package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 휴가 잔액 요약(부여/사용/잔여) 운반체.
 *
 * <p>연차 현황 대시보드(attd09)에서 법정(STATUTORY_*) / 법정외(MANUAL_*) 구분별로 사용한다.
 * 활성(STATUS='ACTIVE' AND DEL_YN='N') 부여 기준이다.
 *
 * <p>사용(used)은 날짜가 도래한 분만 집계하고, 아직 도래하지 않은 미래분은 사용예정(scheduled)으로 분리한다.
 * 두 값 모두 잔여에서는 이미 차감된 상태이므로 잔여는 {@code granted - (used + scheduled)}로 산출한다.
 */
@Getter
@Builder
public class LeaveBalanceVO {

    /** 부여 일수 합계 */
    private final BigDecimal granted;

    /** 사용 일수 합계 (날짜가 도래한 분만. 미도래분은 scheduled로 분리) */
    private final BigDecimal used;

    /** 사용예정 일수 합계 (CONFIRMED 사용 중 아직 날짜가 도래하지 않은 미래분) */
    private final BigDecimal scheduled;

    /** 잔여 일수 (granted - (used + scheduled)) */
    private final BigDecimal remaining;
}
