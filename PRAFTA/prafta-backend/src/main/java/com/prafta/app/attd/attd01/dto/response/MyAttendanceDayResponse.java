package com.prafta.app.attd.attd01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 오늘 탭 / 일상세 공용 응답 (계약 §3.1 = §3.4).
 *
 * <p>GET /attd/my/today, GET /attd/my/day-detail 가 동일 구조로 반환한다.
 * <p>workStatus: BEFORE_WORK/WORKING/TWO_SLOT_WORKING/CHECKED_OUT/CHECK_OUT_MISSING (시안 §2.2).
 *   미래/스케줄없음 등 산출 불가 시 null 일 수 있다.
 */
@Getter
@Builder
public class MyAttendanceDayResponse {
    private final String workDate;       // YYYYMMDD
    private final String siteName;
    private final String workPlanCode;   // SCH_CD 또는 LEAVE_CD
    private final String workPlanName;
    // prafta-app-013: 바텀시트 메타 1줄(이번주 days[] 와 동일) 구성용 요약 문자열.
    //   스케줄 없음/연차일 등 산출 불가 시 null.
    private final String scheduleSummary;
    private final String attendanceSummary;
    private final String workStatus;
    @JsonProperty("isTwoSlot")
    private final boolean isTwoSlot;
    private final List<SlotResponse> slots;
    private final DayActionsResponse actions;
    // prafta-app-013: 오늘/일상세 바텀시트(4액션) 구동용. 이번주 days[].actions 와 동일 구조/규칙.
    //   기존 actions(canRequestModify/canCheckOut/canCheckIn)는 primary 버튼·하위호환으로 유지.
    private final WeekDayActionsResponse sheetActions;
    // 처리 필요(빠른 액션) 판정용 — month days[] 와 동일 규칙(시안 §4.4.3 화면10).
    // WORK/LEAVE/OFF/ACTION_REQUIRED. hasIssue = ACTION_REQUIRED 동치.
    private final String dayType;
    private final boolean hasIssue;
    // prafta-app-003 A0-2: 직전 퇴근이 지오펜스 범위 밖(외근)으로 처리됐는지.
    // check-out 응답에서만 의미를 가진다(조회 경로는 false 로 빌드 — read 의미 재해석은 B에서).
    @JsonProperty("isOffsite")
    private final boolean isOffsite;
}
