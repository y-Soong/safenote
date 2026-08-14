package com.prafta.app.leave.leaveflow.result;

import java.math.BigDecimal;

/**
 * prafta-leavemulti: 연차 기간(From-To) 신청 미리보기 — 구간 내 날짜 1건의 원시 판정 재료.
 *
 * <p>기간신청은 "1 신청 → 날짜별 단일일 신청 N회"로 분해되므로, 미리보기는 각 날짜가
 * ① 신청 가능한지 ② 기본 체크 상태를 무엇으로 둘지 를 판단할 재료를 한 번의 조회로 모은다.
 * (날짜마다 단건 쿼리를 N회 도는 대신 구간 1회 조회 — 14일이면 42회가 1회로 줄어든다.)
 *
 * <p><b>판정 술어는 기존 단일일 가드와 반드시 동일해야 한다</b>(미리보기 ≠ 제출이 되면 안 됨):
 * <ul>
 *   <li>{@code attdCnt} — {@code countAttendanceByDate} 와 동일 (TB_USER_ATTD_MGMT, DEL_YN='N')</li>
 *   <li>{@code occupiedDays} — {@code selectOccupiedLeaveDaysOnDate} 와 동일
 *       (CONFIRMED·DEL_YN='N'·START_DATE~END_DATE 포함, 종일=1.0/그 외=LEAVE_DAYS 합)</li>
 *   <li>{@code hasScheduleYn} — work_plan 행이 있고 그 코드가 실제 근무타입(SCH_CD)일 때만 'Y'
 *       (레거시 휴가코드가 들어있는 셀은 'N')</li>
 * </ul>
 *
 * <p>마감 여부는 월 단위 판정(AttdCloseService)이라 본 행에 포함하지 않고 서비스가 별도로 본다.
 *
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record MultiDayLeaveDayRow(
        /** 날짜 (YYYYMMDD) */
        String ymd
        /** 요일 한 글자 (일~토) — 화면 표기용 */
        , String dow
        /** 주말 여부 ('Y'/'N') — 토·일 */
        , String weekendYn
        /** 공휴일/회사휴일 여부 ('Y'/'N') — TB_HOLIDAY USE_YN='Y' */
        , String holidayYn
        /** 그날 근무계획(SCH)이 배정돼 있는지 ('Y'/'N') — 기본 체크 판정의 1순위 근거 */
        , String hasScheduleYn
        /** 그날 출근 기록 건수 (>0 이면 ATTD_400_108 로 신청 불가) */
        , int attdCnt
        /** 그날 이미 점유된 연차 일수 (>0 이면 종일 신청 시 ATTD_400_111 로 신청 불가) */
        , BigDecimal occupiedDays
) {
}
