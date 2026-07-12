package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 월 일별 셀 (계약 §3.3 days[]).
 *
 * <p>dayType 우선순위 ACTION_REQUIRED > LEAVE > WORK > OFF (plan §0-1/§2.3).
 *   hasIssue = (dayType == ACTION_REQUIRED). 캘린더 셀 색상코딩 근거.
 */
@Getter
@Builder
public class MonthDayResponse {
    private final String workYmd;     // YYYYMMDD
    private final String dayType;     // WORK/LEAVE/OFF/ACTION_REQUIRED
    private final String holidayName;
    private final boolean hasIssue;
    // prafta-app-030 후속: 그날 적용(승인) 초과근무 합계 분(없으면 0). 캘린더 셀 표시 전용 — dayType/hasIssue 무영향.
    private final int overtimeMinutes;
}
