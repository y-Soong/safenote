package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 구간(슬롯) 단위 응답 (계약 §3.1 slots[]).
 * <p>workSeq 1=1구간, 2=2구간. schedule 은 항상, attendance/standardized 는 데이터 유무에 따라.
 *
 * <p>prafta-app-015: 2구간 스케줄 구간 선택 게이팅용 플래그(서버 산출, 프론트 표시 전용).
 *   <ul>
 *     <li>canCheckInThisSlot: 이 구간 지금 출근 가능(2구간 스케줄 한정 산출). 1구간/스케줄없음은 false.</li>
 *     <li>alreadyCheckedIn: 이 구간 이미 출근 등록됨(레코드 존재).</li>
 *   </ul>
 *   ⚠️ is-접두 회피(canCheckInThisSlot/alreadyCheckedIn) — Lombok+Jackson is-탈락 함정 비대상.
 *   직렬화 키는 필드명 그대로(프론트 계약 1:1).
 */
@Getter
@Builder
public class SlotResponse {
    private final int workSeq;
    private final ScheduleResponse schedule;
    private final AttendanceResponse attendance;       // 미출근 시 null
    private final StandardizedResponse standardized;   // 미산정 시 null
    // prafta-app-015: 2구간 스케줄 구간 선택 버튼 게이팅(서버 산출).
    private final boolean canCheckInThisSlot;
    private final boolean alreadyCheckedIn;
}
