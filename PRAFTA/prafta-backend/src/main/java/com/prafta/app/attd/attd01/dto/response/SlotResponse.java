package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 구간(슬롯) 단위 응답 (계약 §3.1 slots[]).
 * <p>workSeq 1=1구간, 2=2구간. schedule 은 항상, attendance/standardized 는 데이터 유무에 따라.
 */
@Getter
@Builder
public class SlotResponse {
    private final int workSeq;
    private final ScheduleResponse schedule;
    private final AttendanceResponse attendance;       // 미출근 시 null
    private final StandardizedResponse standardized;   // 미산정 시 null
}
