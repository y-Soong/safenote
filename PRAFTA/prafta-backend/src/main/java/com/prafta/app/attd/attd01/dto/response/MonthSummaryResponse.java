package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 월 합계 (계약 §3.3 monthlySummary).
 * <p>plannedWorkMinutes=스케줄 합, actualWorkMinutes=완료분만 합 (시안 §3.7).
 */
@Getter
@Builder
public class MonthSummaryResponse {
    private final int plannedWorkMinutes;
    private final int actualWorkMinutes;
}
