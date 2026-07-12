package com.prafta.web.dashboard.dashboard01.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 근태 탭 A4 법정연차 3분할 위젯 응답 (PRAFTA-DASHBOARD-T3).
 * 법정(STATUTORY_%)·STATUS IN ('ACTIVE','EXHAUSTED') 기준 현재 시점 스냅샷.
 * 미사용(unusedDays)은 응답에 포함하지 않는다 — FE 파생 계산(부여−사용−예정, 0 클램프).
 *
 * <p>세 일수는 총량(Σ) 기준이며 게이지 세그먼트와 범례 %의 분모로만 쓴다.
 *   화면에 일수 자체는 표기하지 않는다 — 인원수에 비례해 커져 부서 간 비교가 불가능하고,
 *   시간차 연차가 순환소수로 환산되어 소수점이 길게 붙는다.
 */
@Getter
@Builder
public class LeaveUsageResponse {
    private BigDecimal grantDays;   // 부여 합계 (분모) — Σ GRANT_DAYS
    private BigDecimal usedDays;    // 사용 (START_DATE <= 오늘)
    private BigDecimal plannedDays; // 사용예정 (START_DATE > 오늘)
}
