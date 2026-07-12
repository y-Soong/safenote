package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

/**
 * 그날 시간차(02/03/04) CONFIRMED 누적 집계 (LC-03 — F3 전 타입 합산).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 *
 * @param cumMinutes     그날 시간차 누적 분 합계(LEAVE_MINUTES, NULL→0)
 * @param cumChargedDays 그날 시간차 누적 차감 합계(LEAVE_DAYS, NULL→0)
 */
public record HourlyLeaveAggVO(
      Integer cumMinutes
    , BigDecimal cumChargedDays
) {
}
