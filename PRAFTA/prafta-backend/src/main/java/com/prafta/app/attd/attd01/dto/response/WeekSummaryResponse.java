package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 주 합계 (계약 §3.2 summary).
 * <p>plannedWorkMinutes=스케줄 합. actualWorkMinutes=완료된 근무(출퇴근 모두 등록)만 합 (시안 §3.7).
 */
@Getter
@Builder
public class WeekSummaryResponse {
    private final int plannedWorkMinutes;
    private final int actualWorkMinutes;
    private final String note;
}
