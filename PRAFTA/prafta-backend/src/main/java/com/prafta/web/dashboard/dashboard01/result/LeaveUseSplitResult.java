package com.prafta.web.dashboard.dashboard01.result;

import java.math.BigDecimal;

/**
 * 대시보드 근태 탭 A4 법정연차 사용/사용예정 분리 집계 결과 VO (PRAFTA-DASHBOARD-T3, 조건부 집계 단건).
 * GRANT_ID 경유로 법정 부여(STATUTORY_%·ACTIVE/EXHAUSTED)에 연결된 CONFIRMED 사용분을
 * 오늘 기준(START_DATE) CASE 로 사용/사용예정 분리한다.
 * ⚠ record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서와 1:1 일치 필수.
 */
public record LeaveUseSplitResult(
    BigDecimal usedDays     // START_DATE <= 오늘 (사용)
    , BigDecimal plannedDays // START_DATE >  오늘 (사용예정)
){
}
