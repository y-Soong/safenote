package com.prafta.app.attd.attd01.dto.response;

import java.math.BigDecimal;
import java.util.List;

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
    // PRAFTA_COM_002-B-1: 단건 스칼라(첫 1건) 연차가 승인 대기(요청중)인지. is 탈락 방지(@JsonProperty).
    //   판정=REQ_ID NOT NULL AND REQ_STATUS='01'. 다건은 leaves[].pendingApproval 로 건별 표기.
    @JsonProperty("isLeavePending")
    private final boolean leavePending;
    // 같은 날 부분연차(시간차/반차) 다건 표시용 마커 목록(표시 전용, 시각 오름차순). 사용내역 없으면 빈 리스트.
    //   위 단건 스칼라는 첫 1건 하위호환으로 유지하고, FE 는 이 목록을 우선 렌더한다.
    private final List<LeaveMarkerItem> leaves;
    private final String workPlanCode;
    private final String workPlanName;
    @JsonProperty("isTwoSlot")
    private final boolean isTwoSlot;
    private final String scheduleSummary;
    private final String attendanceSummary;
    private final String attendanceStatus;
    private final WeekDayActionsResponse actions;
    // prafta-app-030 후속: 그날 적용(승인) 초과근무 합계 분(없으면 0) + 항목 목록(없으면 빈 리스트). 표시 전용.
    private final int overtimeMinutes;
    private final List<AppliedOvertimeItem> overtimes;
}
