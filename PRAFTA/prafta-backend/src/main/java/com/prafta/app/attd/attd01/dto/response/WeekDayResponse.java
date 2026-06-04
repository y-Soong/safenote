package com.prafta.app.attd.attd01.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 이번주 일별 요약 (계약 §3.2 days[]).
 *
 * <p>attendanceStatus: NORMAL/LATE/EARLY_LEAVE/MISSING/WORKING/NOT_STARTED (attd §10.1).
 *   scheduleSummary 예) "0930~1800" 또는 2구간 "0700~1300 / 1700~2100".
 *   attendanceSummary 미래/미생성 시 null.
 */
@Getter
@Builder
public class WeekDayResponse {
    private final String workYmd;        // YYYYMMDD
    private final String dayOfWeek;      // MON~SUN
    // Jackson 이 boolean is* getter 에서 "is" 를 떼고 직렬화하는 것을 방지(계약 키 고정).
    @JsonProperty("isToday")
    private final boolean isToday;
    @JsonProperty("isHoliday")
    private final boolean isHoliday;
    private final String holidayName;
    @JsonProperty("isLeaveUsed")
    private final boolean isLeaveUsed;
    private final String leaveTypeName;
    // prafta-app-018-E: 부분연차(시간차/반차) 상세 표시필드. 라벨 산출은 FE(attdFormat.js).
    //   leaveUnitType=SYS025 코드 원값, leaveTimeRange="HHMM~HHMM"(둘 다 있을 때만, 없으면 null),
    //   leaveDays=차감일수 원값(정규화는 FE). 연차 미사용일이면 모두 null.
    private final String leaveUnitType;
    private final String leaveTimeRange;
    private final BigDecimal leaveDays;
    private final String workPlanCode;
    private final String workPlanName;
    @JsonProperty("isTwoSlot")
    private final boolean isTwoSlot;
    private final String scheduleSummary;
    private final String attendanceSummary;
    private final String attendanceStatus;
    private final WeekDayActionsResponse actions;
}
