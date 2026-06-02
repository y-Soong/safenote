package com.prafta.app.attd.attd01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 이번주 응답 루트 (계약 §3.2).
 * <p>GET /attd/my/week. days 는 weekStartYmd 기준 7일.
 */
@Getter
@Builder
public class MyWeekResponse {
    private final String weekStartYmd;
    private final String weekEndYmd;
    private final List<WeekDayResponse> days;
    private final WeekSummaryResponse summary;
}
