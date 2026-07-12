package com.prafta.app.attd.attd01.dto.response;

import java.math.BigDecimal;
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
    // prafta-app-014: effectiveSlotCount(1~2). 프론트 구분선/슬롯 반복·상태·버튼의 단일 기준.
    //   isTwoSlot 은 "스케줄이 2구간인가"(현행 의미 유지)로 남기고, 슬롯/상태/버튼 판정은 slotCount 로 이관.
    private final int slotCount;
    private final List<SlotResponse> slots;
    private final DayActionsResponse actions;
    // prafta-app-013: 오늘/일상세 바텀시트(4액션) 구동용. 이번주 days[].actions 와 동일 구조/규칙.
    //   기존 actions(canRequestModify/canCheckOut/canCheckIn)는 primary 버튼·하위호환으로 유지.
    private final WeekDayActionsResponse sheetActions;
    // 처리 필요(빠른 액션) 판정용 — month days[] 와 동일 규칙(시안 §4.4.3 화면10).
    // WORK/LEAVE/OFF/ACTION_REQUIRED. hasIssue = ACTION_REQUIRED 동치.
    private final String dayType;
    private final boolean hasIssue;
    // 휴일(웹 휴일관리 TB_HOLIDAY) 반영 — 주/월 응답과 동일 계약. isHoliday 는 is 탈락 방지(@JsonProperty).
    //   holidayName 은 휴일명(공휴일/회사휴일), 휴일 아니면 null.
    @JsonProperty("isHoliday")
    private final boolean isHoliday;
    private final String holidayName;
    // prafta-app-003 A0-2: 직전 퇴근이 지오펜스 범위 밖(외근)으로 처리됐는지.
    // check-out 응답에서만 의미를 가진다(조회 경로는 false 로 빌드 — read 의미 재해석은 B에서).
    @JsonProperty("isOffsite")
    private final boolean isOffsite;
    // prafta-app-018-E: 부분연차(시간차/반차) 상세 표시필드(표시 전용, 근무일 유지 — slot/dayType/status 무영향).
    //   isLeaveUsed: 그날 CONFIRMED 연차 사용 존재 여부(@JsonProperty 로 is 탈락 방지, 계약 키 고정).
    //   leaveUnitType=SYS025 코드 원값(라벨은 FE), leaveTimeRange="HHMM~HHMM"(둘 다 있을 때만; 없으면 null),
    //   leaveDays=차감일수 원값(정규화 FE), leaveTypeName=연차종류명. 연차 미사용일이면 전부 null/false.
    @JsonProperty("isLeaveUsed")
    private final boolean isLeaveUsed;
    private final String leaveTypeName;
    private final String leaveUnitType;
    private final String leaveTimeRange;
    private final BigDecimal leaveDays;
    // PRAFTA_COM_002-B-1: 단건 스칼라(첫 1건) 연차가 승인 대기(요청중)인지. is 탈락 방지(@JsonProperty).
    //   판정=REQ_ID NOT NULL AND REQ_STATUS='01'. 다건은 leaves[].pendingApproval 로 건별 표기.
    @JsonProperty("isLeavePending")
    private final boolean leavePending;
    // 같은 날 부분연차(시간차/반차) 다건 표시용 마커 목록(표시 전용, 시각 오름차순). 사용내역 없으면 빈 리스트.
    //   위 단건 스칼라(leaveTypeName 등)는 첫 1건 하위호환으로 유지하고, FE 는 이 목록을 우선 렌더한다.
    private final List<LeaveMarkerItem> leaves;
    // prafta-app-030 후속: 그날 적용(승인) 초과근무 목록(없으면 빈 리스트). 표시 전용 — 상태/슬롯/액션 무영향.
    private final List<AppliedOvertimeItem> appliedOvertimes;
}
