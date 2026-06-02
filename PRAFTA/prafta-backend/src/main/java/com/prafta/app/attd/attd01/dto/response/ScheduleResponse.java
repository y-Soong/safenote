package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 슬롯(구간)별 스케줄 정보 응답 (계약 §3.1 slots[].schedule).
 * <p>시간은 HHMM, breakMinutes/workMinutes 는 서버 산출 분(int).
 */
@Getter
@Builder
public class ScheduleResponse {
    private final String startTime;     // HHMM
    private final String endTime;       // HHMM
    private final Integer breakMinutes; // 휴게분
    private final Integer workMinutes;  // (종료-시작-휴게), attd §10.1/§10.4
}
