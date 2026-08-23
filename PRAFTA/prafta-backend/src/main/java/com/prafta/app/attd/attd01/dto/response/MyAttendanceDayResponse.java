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
    /**
     * PRAFTA-FIXEDOT-2(표기): 고정연장 요약 — scheduleSummary 와 동일 형상(raw HHMM).
     * 고정연장 없는 근무타입은 null(미표기 — 무회귀). WeekDayResponse.fixedOtSummary 와 동일 규약.
     */
    private final String fixedOtSummary;
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
    // 작업지시서_연차변경화면_진입버튼: 연차 이동(TARGET_LEAVE_ID) 발의용 식별자 + 이동 가능 여부.
    //   leaveId=TB_USER_LEAVE_USE.LEAVE_ID(연차 미사용일이면 null). leaveMovable=미래/오늘 + CONFIRMED 파생값.
    private final String leaveId;
    private final boolean leaveMovable;
    // PRAFTA_COM_002-B-1: 단건 스칼라(첫 1건) 연차가 승인 대기(요청중)인지. is 탈락 방지(@JsonProperty).
    //   판정=REQ_ID NOT NULL AND REQ_STATUS='01'. 다건은 leaves[].pendingApproval 로 건별 표기.
    @JsonProperty("isLeavePending")
    private final boolean leavePending;
    // 같은 날 부분연차(시간차/반차) 다건 표시용 마커 목록(표시 전용, 시각 오름차순). 사용내역 없으면 빈 리스트.
    //   위 단건 스칼라(leaveTypeName 등)는 첫 1건 하위호환으로 유지하고, FE 는 이 목록을 우선 렌더한다.
    private final List<LeaveMarkerItem> leaves;
    // prafta-app-030 후속: 그날 적용(승인) 초과근무 목록(없으면 빈 리스트). 표시 전용 — 상태/슬롯/액션 무영향.
    private final List<AppliedOvertimeItem> appliedOvertimes;

    /**
     * OT 칩 정합(2026-08-08): 그날 확정 부분연차(반차/시간차)의 면제 구간 목록 — 초과근무 신청 폼의
     * "등록 가능" 칩이 FE 계산(실근태−스케줄)에 이 구간을 추가로 빼서 서버 검증(ATTD_400_196)과
     * 일치시킨다. 산출 = 검증과 동일 단일 진입점(PartialLeaveWindowUtils.exemptStampRange + 그날
     * 스케줄 프레임). 환산 불가 행은 그날 전체 구간으로 보수 변환(검증의 전면 거부와 정합).
     * additive — 구 FE 는 무시. 없으면 빈 리스트.
     */
    private final List<LeaveExemptWindowItem> leaveExemptWindows;

    /**
     * PRAFTA-FIXEDOT-2(정책 ①·④): 그날 스케줄의 고정연장(전방·후방) 점유 구간 목록 — 초과근무
     * 신청 폼의 "등록 가능" 칩이 연차 면제 구간과 동일하게 피감수에 합쳐(additive) 서버 검증
     * (ATTD_400_100 고정연장 겹침 거부)과 일치시킨다. 표현/축은 leaveExemptWindows 와 동일
     * ((일자,시각) 쌍 — 아이템 record 재사용). 고정연장 없는 근무타입/구 FE 는 빈 리스트/무시.
     */
    private final List<LeaveExemptWindowItem> fixedOtWindows;

    // 작업지시서_소속이동-이력가시성-보정 T3: "당시 소속 사업장" 배지 데이터 소스(기존 siteName과 무관 — 그건 현재 사업장).
    //   레코드 자체 SITE_CD 기준(실 근태 우선, 없으면 배정 스케줄). 둘 다 없으면 null(배지 미노출 신호).
    private final String recordSiteCd;
    private final String recordSiteName;
}
