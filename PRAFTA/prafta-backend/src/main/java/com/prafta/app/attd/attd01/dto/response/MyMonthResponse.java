package com.prafta.app.attd.attd01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 이번달 응답 루트 (계약 §3.3).
 * <p>GET /attd/my/month. days 는 해당 월의 1일~말일.
 */
@Getter
@Builder
public class MyMonthResponse {
    private final String yearMonth;   // YYYYMM
    private final MonthSummaryResponse monthlySummary;
    private final List<MonthDayResponse> days;
}
